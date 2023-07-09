// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes.ATX_1
import org.intellij.markdown.MarkdownElementTypes.ATX_2
import org.intellij.markdown.MarkdownElementTypes.ATX_3
import org.intellij.markdown.MarkdownElementTypes.LINK_DEFINITION
import org.intellij.markdown.MarkdownElementTypes.LINK_DESTINATION
import org.intellij.markdown.MarkdownElementTypes.LINK_LABEL
import org.intellij.markdown.MarkdownElementTypes.ORDERED_LIST
import org.intellij.markdown.MarkdownElementTypes.PARAGRAPH
import org.intellij.markdown.MarkdownElementTypes.UNORDERED_LIST
import org.intellij.markdown.MarkdownTokenTypes.Companion.EOL
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.changelog.ChangelogPluginConstants.DEFAULT_TITLE
import org.jetbrains.changelog.ChangelogPluginConstants.LEVEL_1
import org.jetbrains.changelog.ChangelogPluginConstants.LEVEL_2
import org.jetbrains.changelog.ChangelogPluginConstants.LEVEL_3
import org.jetbrains.changelog.ChangelogPluginConstants.SEM_VER_REGEX
import org.jetbrains.changelog.exceptions.HeaderParseException
import org.jetbrains.changelog.exceptions.MissingVersionException
import org.jetbrains.changelog.flavours.ChangelogFlavourDescriptor

@Suppress("MemberVisibilityCanBePrivate")
data class Changelog(
    private val content: String,
    private val defaultPreTitle: String?,
    private val defaultTitle: String?,
    private val defaultIntroduction: String?,
    private val unreleasedTerm: String,
    private val groups: List<String>,
    private val headerParserRegex: Regex,
    private val itemPrefix: String,
    private val repositoryUrl: String?,
    private val sectionUrlBuilder: ChangelogSectionUrlBuilder,
    private val lineSeparator: String,
) {

    val preTitle: String
    val title: String
    val introduction: String
    var items: Map<String, Item>
        internal set

    private val baseItems: Map<String, List<ASTNode>>
    internal val baseLinks: MutableList<Pair<String, String>>
    internal val newUnreleasedItem
        get() = Item(
            version = unreleasedTerm,
            header = unreleasedTerm,
            items = groups.associateWith { emptySet() },
            itemPrefix = itemPrefix,
            lineSeparator = lineSeparator,
        ).withEmptySections(true)

    init {
        val tree = parseTree(content)
        val nodes = tree?.children
            .orEmpty()
            .filterNot { it.type == EOL }

        val preTitleNodes = nodes
            .takeWhile { it.type != ATX_1 }
            .takeIf { nodes.any { it.type == ATX_1 } }
            .orEmpty()

        val titleNode = nodes
            .find { it.type == ATX_1 }

        val introductionNodes = nodes
            .dropWhile { it.type != ATX_1 }
            .dropWhile { it.type == ATX_1 }
            .takeWhile { it.type != ATX_2 }

        preTitle = defaultPreTitle
            .orDefault(preTitleNodes.join())
        title = defaultTitle
            .orDefault(titleNode.text())
            .orDefault(DEFAULT_TITLE)
        introduction = defaultIntroduction
            .orDefault(introductionNodes.join().reformat(lineSeparator))

        baseItems = nodes
            .dropWhile { it.type != ATX_2 }
            .filterNot { it.type == LINK_DEFINITION }
            .groupByType(ATX_2) {
                with(it.text()) {
                    when {
                        contains(unreleasedTerm) -> unreleasedTerm
                        else -> split("""[^-+.0-9a-zA-Z]+""".toRegex())
                            .firstOrNull(headerParserRegex::matches)
                            ?: throw HeaderParseException(this, unreleasedTerm)
                    }
                }
            }
            .mapKeys {
                headerParserRegex
                    .matchEntire(it.key)
                    ?.run { groupValues.drop(1).firstOrNull() }
                    ?: it.key
            }

        baseLinks = nodes.extractLinks().toMutableList()

        items = baseItems.mapValues { (key, value) ->
            val header = value
                .firstOrNull { it.type == ATX_2 }
                .text()
                .replace("[$key]", key)
            val isUnreleased = key == unreleasedTerm

            val (summary, items) = value.extractItemData()

            Item(key, header, summary, isUnreleased, items, itemPrefix, lineSeparator)
                .withEmptySections(isUnreleased)
        }
    }

    val links
        get() = sequence {
            repositoryUrl?.let {
                val build = sectionUrlBuilder::build
                val ids = items.keys - unreleasedTerm

                with(unreleasedTerm) {
                    if (items.containsKey(this)) {
                        val url = build(repositoryUrl, this, ids.firstOrNull(), true)
                        yield(this to url)
                    }
                }

                ids.windowed(2).forEach { (current, previous) ->
                    val url = build(repositoryUrl, current, previous, false)
                    yield(current to url)
                }

                ids.lastOrNull()?.let {
                    val url = build(repositoryUrl, it, null, false)
                    yield(it to url)
                }
            }

            yieldAll(baseLinks)
        }
            .sortedWith { (left), (right) ->
                val leftIsSemVer = SEM_VER_REGEX.matches(left)
                val rightIsSemVer = SEM_VER_REGEX.matches(right)
                val leftVersion = Version.parse(left)
                val rightVersion = Version.parse(right)

                when {
                    left == unreleasedTerm -> -1
                    right == unreleasedTerm -> 1
                    leftIsSemVer && rightIsSemVer -> rightVersion.compareTo(leftVersion)
                    leftIsSemVer -> -1
                    rightIsSemVer -> 1
                    else -> left.compareTo(right)
                }
            }
            .toMap()

    val unreleasedItem: Item?
        get() = items[unreleasedTerm]

    val releasedItems
        get() = items
            .filterKeys { it != unreleasedTerm }
            .values

    fun get(version: String) = items[version] ?: throw MissingVersionException(version)

    fun getLatest() = releasedItems.firstOrNull() ?: throw MissingVersionException("any")

    fun has(version: String) = items.containsKey(version)

    fun renderItem(item: Item, outputType: OutputType) = with(item) {
        sequence {
            if (withHeader) {
                when {
                    withLinkedHeader && links.containsKey(version) -> yield("$LEVEL_2 ${header.replace(version, "[$version]")}")
                    else -> yield("$LEVEL_2 $header")
                }
            }

            if (withSummary && summary.isNotEmpty()) {
                yield(summary)
                yield(lineSeparator)
            }

            sections
                .entries
                .asSequence()
                .iterator()
                .run {
                    while (hasNext()) {
                        val (section, entries) = next()

                        if (section.isNotEmpty()) {
                            yield("$LEVEL_3 $section")
                        }
                        entries
                            .map { "$itemPrefix $it" }
                            .let { yieldAll(it) }
                    }
                }

            if (withLinks) {
                links
                    .filterKeys { id ->
                        id == version
                                || (withSummary && summary.contains("[$id]")
                                || sections.flatMap { it.value }.any { it.contains("[$id]") })
                    }
                    .forEach { (id, url) ->
                        yield("[$id]: $url")
                    }
            }
        }
    }
        .joinToString(lineSeparator)
        .reformat(lineSeparator)
        .processOutput(outputType)

    fun render(outputType: OutputType) = sequence {
        if (preTitle.isNotBlank()) {
            yield(preTitle)
        }
        if (title.isNotBlank()) {
            yield("$LEVEL_1 $title")
        }
        if (introduction.isNotBlank()) {
            yield(introduction)
        }

        unreleasedItem?.let {
            yield(renderItem(it.withLinks(false), OutputType.MARKDOWN))
        }

        releasedItems.forEach {
            yield(renderItem(it.withLinks(false), OutputType.MARKDOWN))
        }

        links.forEach { (id, url) ->
            yield("[$id]: $url")
        }
    }
        .joinToString(lineSeparator)
        .reformat(lineSeparator)
        .processOutput(outputType)

    data class Item(
        var version: String,
        val header: String,
        val summary: String = "",
        val isUnreleased: Boolean = false,
        private val items: Map<String, Set<String>> = emptyMap(),
        private val itemPrefix: String,
        private val lineSeparator: String,
    ) {

        internal var withHeader = true
        internal var withLinkedHeader = true
        internal var withSummary = true
        internal var withLinks = true
        private var withEmptySections = false
        private var filterCallback: ((String) -> Boolean)? = null

        fun withHeader(header: Boolean) = copy(withHeader = header)

        fun withLinkedHeader(linkedHeader: Boolean) = copy(withLinkedHeader = linkedHeader)

        fun withSummary(summary: Boolean) = copy(withSummary = summary)

        fun withLinks(links: Boolean) = copy(withLinks = links)

        fun withEmptySections(emptySections: Boolean) = copy(withEmptySections = emptySections)

        fun withFilter(filter: ((String) -> Boolean)?) = copy(filterCallback = filter)

        var sections = emptyMap<String, Set<String>>()
            internal set
            get() = items
                .mapValues { it.value.filter { item -> filterCallback?.invoke(item) ?: true }.toSet() }
                .filter { it.value.isNotEmpty() || withEmptySections }

        private fun copy(
            version: String = this.version,
            header: String = this.header,
            summary: String = this.summary,
            isUnreleased: Boolean = this.isUnreleased,
            items: Map<String, Set<String>> = this.items,
            withHeader: Boolean = this.withHeader,
            withLinkedHeader: Boolean = this.withLinkedHeader,
            withSummary: Boolean = this.withSummary,
            withLinks: Boolean = this.withLinks,
            withEmptySections: Boolean = this.withEmptySections,
            filterCallback: ((String) -> Boolean)? = this.filterCallback,
        ) = Item(
            version,
            header,
            summary,
            isUnreleased,
            items,
            itemPrefix,
            lineSeparator,
        ).also {
            it.withHeader = withHeader
            it.withLinkedHeader = withLinkedHeader
            it.withSummary = withSummary
            it.withLinks = withLinks
            it.withEmptySections = withEmptySections
            it.filterCallback = filterCallback
        }

        operator fun plus(item: Item?): Item {
            if (item == null) {
                return copy()
            }

            return copy(
                summary = summary.ifEmpty { item.summary },
                items = items + item.items,
                filterCallback = filterCallback,
            )
        }

        operator fun plus(items: List<Item>) = items.fold(this) { acc, item -> acc + item }

        operator fun Map<String, Set<String>>.plus(other: Map<String, Set<String>>) = this
            .mapValues { (key, value) -> value + other[key].orEmpty() }
            .toMutableMap()
            .also { map -> map.putAll(other.filterKeys { !containsKey(it) }) }

        @Deprecated(
            message = "Method no longer available",
            replaceWith = ReplaceWith("changelog.renderItem(this)"),
        )
        fun toText() = ""

        @Deprecated(
            message = "Method no longer available",
            replaceWith = ReplaceWith("changelog.renderItem(this, Changelog.OutputType.HTML)"),
        )
        fun toHTML() = ""

        @Deprecated(
            message = "Method no longer available",
            replaceWith = ReplaceWith("changelog.renderItem(this, Changelog.OutputType.PLAIN_TEXT)"),
        )
        fun toPlainText() = ""

        @Deprecated(
            message = "Method no longer available",
            replaceWith = ReplaceWith("changelog.renderItem(this)"),
        )
        override fun toString() = ""
    }

    enum class OutputType {
        MARKDOWN,
        PLAIN_TEXT,
        HTML,
    }

    private fun ASTNode?.textAsIs(customContent: String? = null) = this
        ?.getTextInNode(customContent ?: content)
        ?.let {
            when (type) {
                EOL -> lineSeparator
                else -> it
            }
        }
        ?.toString()
        .orEmpty()

    private fun ASTNode?.text(customContent: String? = null) = this
        ?.getTextInNode(customContent ?: content)
        ?.let {
            when (type) {
                ATX_1 -> it.removePrefix(LEVEL_1)
                ATX_2 -> it.removePrefix(LEVEL_2)
                ATX_3 -> it.removePrefix(LEVEL_3)
                else -> it
            }
        }
        ?.toString()
        .orEmpty()
        .trim()

    private fun List<ASTNode>.groupByType(
        type: IElementType,
        getKey: ((item: ASTNode) -> String)? = null,
    ): Map<String, List<ASTNode>> {
        var key = ""
        return groupBy {
            if (it.type == type) {
                key = getKey?.invoke(it) ?: it.text()
            }
            key
        }
    }

    private fun String?.orDefault(defaultValue: String?) = this
        .orEmpty()
        .ifBlank { defaultValue }
        .orEmpty()
        .trim()

    private fun String.processOutput(outputType: OutputType) = when (outputType) {
        OutputType.MARKDOWN -> this.normalizeLineSeparator(lineSeparator) // Ensure that output content has always the correct line separator
        OutputType.HTML -> markdownToHTML(this, lineSeparator)
        OutputType.PLAIN_TEXT -> markdownToPlainText(this, lineSeparator)
    }

    internal fun parseTree(content: String?) =
        content?.let {
            MarkdownParser(ChangelogFlavourDescriptor())
                .buildMarkdownTreeFromString(content)
        }

    private fun List<ASTNode>.join() = joinToString(lineSeparator) { it.text() }

    internal fun List<ASTNode>.extractItemData(content: String? = null): Pair<String, Map<String, Set<String>>> {
        val summaryNodes = this
            .filter { it.type != ATX_2 }
            .takeWhile { it.type == PARAGRAPH }
        val summary = summaryNodes
            .joinToString("$lineSeparator$lineSeparator") { it.text(content) }

        val sectionNodes = this
            .filter { it.type != ATX_2 }
            .filter { it.type == ATX_3 || it.type == UNORDERED_LIST || it.type == ORDERED_LIST }

        val items = with(sectionNodes) {
            val unassignedItems = mapOf(
                "" to this
                    .takeWhile { it.type != ATX_3 }
                    .flatMap { list ->
                        list.children
                            .filter { it.type == org.intellij.markdown.MarkdownElementTypes.LIST_ITEM }
                            .map { listItem ->
                                listItem.children
                                    .drop(1) // MarkdownTokenTypes.LIST_BULLET or LIST_NUMBER
                                    .joinToString("") { it.textAsIs(content) }
                            }
                            .toSet()
                    }
                    .toSet()
            )
            val sectionPlaceholders = this
                .filter { it.type == ATX_3 }
                .associate { it.text(content) to emptySet<String>() }
            val sectionsWithItems = this
                .windowed(2)
                .filter { (left, right) -> left.type == ATX_3 && right.type != ATX_3 }
                .associate { (left, right) ->
                    left.text(content) to right.children
                        .filter { it.type == org.intellij.markdown.MarkdownElementTypes.LIST_ITEM }
                        .map { listItem ->
                            listItem.children
                                .drop(1) // MarkdownTokenTypes.LIST_BULLET or LIST_NUMBER
                                .joinToString("") { it.textAsIs(content) }
                        }
                        .toSet()
                }

            unassignedItems + sectionPlaceholders + sectionsWithItems
        }

        return summary to items
    }

    internal fun List<ASTNode>.extractLinks(content: String? = null) = this
        .filter { it.type == LINK_DEFINITION }
        .map { node ->
            node.children.run {
                val label = find { it.type == LINK_LABEL }.text(content).trim('[', ']')
                val destination = find { it.type == LINK_DESTINATION }.text(content)
                label to destination
            }
        }
}
