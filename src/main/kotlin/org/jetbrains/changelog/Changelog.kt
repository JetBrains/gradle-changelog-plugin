// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes.ATX_1
import org.intellij.markdown.MarkdownElementTypes.ATX_2
import org.intellij.markdown.MarkdownElementTypes.ATX_3
import org.intellij.markdown.MarkdownElementTypes.LINK_DEFINITION
import org.intellij.markdown.MarkdownElementTypes.LINK_DESTINATION
import org.intellij.markdown.MarkdownElementTypes.LINK_LABEL
import org.intellij.markdown.MarkdownElementTypes.LIST_ITEM
import org.intellij.markdown.MarkdownElementTypes.ORDERED_LIST
import org.intellij.markdown.MarkdownElementTypes.PARAGRAPH
import org.intellij.markdown.MarkdownElementTypes.UNORDERED_LIST
import org.intellij.markdown.MarkdownTokenTypes.Companion.EOL
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.changelog.ChangelogPluginConstants.LEVEL_1
import org.jetbrains.changelog.ChangelogPluginConstants.LEVEL_2
import org.jetbrains.changelog.ChangelogPluginConstants.LEVEL_3
import org.jetbrains.changelog.exceptions.HeaderParseException
import org.jetbrains.changelog.exceptions.MissingFileException
import org.jetbrains.changelog.exceptions.MissingVersionException
import org.jetbrains.changelog.flavours.ChangelogFlavourDescriptor
import java.io.File
import java.lang.Thread.yield

data class Changelog(
    val file: File,
    val defaultPreTitle: String?,
    val defaultTitle: String?,
    val defaultIntroduction: String?,
    val unreleasedTerm: String,
    val groups: List<String>,
    val headerParserRegex: Regex,
    val itemPrefix: String,
    val repositoryUrl: String?,
    val sectionUrlBuilder: ChangelogSectionUrlBuilder,
    val lineSeparator: String,
) {

    private val flavour = ChangelogFlavourDescriptor()
    private val parser = MarkdownParser(flavour)

    val content = file.run {
        if (!exists()) {
            throw MissingFileException(canonicalPath)
        }
        readText()
    }

    private val tree = parser.buildMarkdownTreeFromString(content)
    private val nodes = tree.children
        .filterNot { it.type == EOL }

    private val preTitleNodes = nodes
        .takeWhile { it.type != ATX_1 }
        .takeIf { nodes.any { it.type == ATX_1 } }
        .orEmpty()
    val preTitle = defaultPreTitle
        .orDefault(
            preTitleNodes.joinToString(lineSeparator) { it.text() }
        )

    private val titleNode = nodes
        .find { it.type == ATX_1 }
    val title = defaultTitle
        .orDefault(
            titleNode.text()
        )

    private val introductionNodes = nodes
        .dropWhile { it.type != ATX_1 }
        .dropWhile { it.type == ATX_1 }
        .takeWhile { it.type != ATX_2 }
    val introductionValue = defaultIntroduction
        .orDefault(
            introductionNodes
                .joinToString(lineSeparator) { it.text() }
                .reformat(lineSeparator)
        )

    private val itemsNodes = nodes
        .dropWhile { it.type != ATX_2 }
        .filterNot { it.type == LINK_DEFINITION }
    private val itemsTemp = itemsNodes
        .dropWhile { it.type != ATX_2 }
        .takeWhile { it.type != LINK_DEFINITION }
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

    private val linkNodes = nodes
        .filter { it.type == LINK_DEFINITION }
    private val links
        get() {
            val currentLinks = linkNodes
                .associate { definition ->
                    definition.children.run {
                        val label = find { it.type == LINK_LABEL }.text()
                        val destination = find { it.type == LINK_DESTINATION }.text()
                        label to destination
                    }
                }

//            val generatedLinks = sectionNodes.associate {
//                it.text() to sectionUrlBuilder.build(repositoryUrl)
//            }

            val generatedLinks = repositoryUrl?.let {
                val build = sectionUrlBuilder::build
                val ids = itemsTemp.keys

                sequence {
                    unreleasedItem.let {
                        val url = build(repositoryUrl, it.version, ids.first(), true)
                        yield(it to "[${it.version}]: $url")
                    }

                    ids.windowed(2).forEach { (current, previous) ->
                        val url = build(repositoryUrl, current, previous, false)
                        yield(current to "[${current}]: $url")
                    }

                    ids.last().let {
                        val url = build(repositoryUrl, it, null, false)
                        yield(it to "[$it]: $url")
                    }
                }.toMap()
            } ?: emptyMap()

            currentLinks + generatedLinks
        }

    val items = itemsTemp
        .mapValues { (key, value) ->
            val header = value
                .firstOrNull { it.type == ATX_2 }.text()
            val isUnreleased = key == unreleasedTerm

            val nodes = value
                .filter { it.type != ATX_2 }

            val summaryNodes = nodes
                .takeWhile { it.type == PARAGRAPH }
            val summary = summaryNodes
                .joinToString("$lineSeparator$lineSeparator") { it.text() }

            val sectionNodes = nodes
                .filter { it.type == ATX_3 || it.type == UNORDERED_LIST || it.type == ORDERED_LIST }

            val items = with(sectionNodes) {
                val unassignedItems = mapOf(
                    "" to sectionNodes
                        .takeWhile { it.type != ATX_3 }
                        .flatMap { list ->
                            list.children
                                .filter { it.type == LIST_ITEM }
                                .map { it.children.last().text() }
                                .toSet()
                        }
                        .toSet()
                )
                val sectionPlaceholders = sectionNodes
                    .filter { it.type == ATX_3 }
                    .associate { it.text() to emptySet<String>() }
                val sectionsWithItems = sectionNodes
                    .windowed(2)
                    .filter { (left, right) -> left.type == ATX_3 && right.type != ATX_3 }
                    .associate { (left, right) ->
                        left.text() to right.children
                            .filter { it.type == LIST_ITEM }
                            .map { it.children.last().text() }
                            .toSet()
                    }

                unassignedItems + sectionPlaceholders + sectionsWithItems
            }

            Item(key, header, summary, isUnreleased, items, links, itemPrefix, lineSeparator)
        }

    val unreleasedItem
        get() = items[unreleasedTerm]
            ?: Item(
                version = unreleasedTerm,
                header = unreleasedTerm,
                items = groups.associateWith { emptySet() },
                itemPrefix = itemPrefix,
                lineSeparator = lineSeparator,
            )
                .withLinks(repositoryUrl != null)

    val releasedItems
        get() = items
            .filterKeys { it != unreleasedTerm }
            .values

    fun has(version: String) = items.containsKey(version)

    fun get(version: String) = items[version] ?: throw MissingVersionException(version)

    fun getAll() = items

    fun getLatest() = items[items.keys.first()] ?: throw MissingVersionException("any")

//    fun patch(version: String, releaseNote: String) {
//        val item = get(version)
//        val newContent = content.replace(item.content, item.patch(patch))
//        file.writeText(newContent)
//    }

    fun render(sectionUrlBuilder: ChangelogSectionUrlBuilder) = sequence {
        if (preTitle.isNotBlank()) {
            yield(preTitle)
        }
        if (title.isNotBlank()) {
            yield("$LEVEL_1 $title")
        }
        if (!defaultIntroduction.isNullOrBlank()) {
            yield(defaultIntroduction)
        }

//        unreleasedItem
//            .withLinks(false)
//            .withEmptySections(true)
//            .let { yield(it) }

//        function()
//        yield("${ChangelogPluginConstants.LEVEL_2} ${item.header}")
//
//        if (content.isNotBlank()) {
//            yield(content)
//        } else {
//            yield(item.withHeader(false).toString())
//        }

        items.values.forEach {
            yield(it.withLinks(false))
        }

        repositoryUrl?.let {
            val build = sectionUrlBuilder::build

            unreleasedItem.let {
                val url = build(repositoryUrl, it.version, items.values.first().version, true)
                yield("[${it.version}]: $url")
            }

            items.values.windowed(2).forEach { (current, previous) ->
                val url = build(repositoryUrl, current.version, previous.version, false)
                yield("[${current.version}]: $url")
            }

            items.values.last().let {
                val url = build(repositoryUrl, it.version, null, false)
                yield("[${it.version}]: $url")
            }
        }
    }
        .joinToString(lineSeparator)
        .reformat(lineSeparator)

    data class Item(
        var version: String,
        val header: String,
        val summary: String = "",
        val isUnreleased: Boolean = false,
        private val items: Map<String, Set<String>> = emptyMap(),
        private val links: Map<String, String> = emptyMap(),
        private val itemPrefix: String,
        private val lineSeparator: String,
    ) {

        private var withHeader = true
        private var withSummary = true
        private var withLinks = true
        private var withEmptySections = false
        private var filterCallback: ((String) -> Boolean)? = null

        val urlifiedHeader = with("[$version]") {
            when {
                header.contains(this) -> header
                else -> header.replace(version, this)
            }
        }

        fun withHeader(header: Boolean) = copy(withHeader = header)

        fun withSummary(summary: Boolean) = copy(withSummary = summary)

        fun withLinks(links: Boolean) = copy(withLinks = links)

        fun withEmptySections(emptySections: Boolean) = copy(withEmptySections = emptySections)

        fun withFilter(filter: ((String) -> Boolean)?) = copy(filterCallback = filter)

        val sections
            get() = items
                .mapValues { it.value.filter { item -> filterCallback?.invoke(item) ?: true } }
                .filter { it.value.isNotEmpty() || withEmptySections }

        fun toText() = sequence {
            if (withHeader) {
                val hasHeaderLink = links.isNotEmpty() && links.containsKey(urlifiedHeader)

                when {
                    withLinks && hasHeaderLink -> yield("$LEVEL_2 $urlifiedHeader")
                    else -> yield("$LEVEL_2 $header")
                }
            }

            if (withSummary && summary.isNotEmpty()) {
                yield(summary)
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

                        yield(lineSeparator)
                        entries
                            .map { "$itemPrefix $it" }
                            .let { yieldAll(it) }
                    }
                }
        }
            .joinToString(lineSeparator)
            .let { content ->
                when {
                    !withLinks -> content
                    else -> {
                        val links = links
                            .filter { (label) -> content.contains(label) }
                            .map { (label, destination) -> "$label: $destination" }
                            .joinToString(lineSeparator)

                        content + lineSeparator + links
                    }
                }
            }
            .reformat(lineSeparator)

        fun toHTML() = markdownToHTML(toText())

        fun toPlainText() = markdownToPlainText(toText(), lineSeparator)

        override fun toString() = toText()

        private fun copy(
            version: String = this.version,
            header: String = this.header,
            summary: String = this.summary,
            isUnreleased: Boolean = this.isUnreleased,
            items: Map<String, Set<String>> = this.items,
            links: Map<String, String> = this.links,
            withHeader: Boolean = this.withHeader,
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
            links,
            itemPrefix,
            lineSeparator,
        ).also {
            it.withHeader = withHeader
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
                links = links + item.links,
            )
        }

        operator fun Map<String, Set<String>>.plus(other: Map<String, Set<String>>) = this
            .mapValues { (key, value) -> value + other[key].orEmpty() }
            .toMutableMap()
            .also { map -> map.putAll(other.filterKeys { !containsKey(it) }) }

        operator fun plus(items: List<Item>) = items.fold(this) { acc, item -> acc + item }
    }

    private fun ASTNode?.text() = this
        ?.getTextInNode(content)
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
}
