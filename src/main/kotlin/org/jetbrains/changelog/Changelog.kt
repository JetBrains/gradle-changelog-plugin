// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.changelog.ChangelogPluginConstants.ATX_2
import org.jetbrains.changelog.ChangelogPluginConstants.ATX_3
import org.jetbrains.changelog.exceptions.HeaderParseException
import org.jetbrains.changelog.exceptions.MissingFileException
import org.jetbrains.changelog.exceptions.MissingVersionException
import org.jetbrains.changelog.flavours.ChangelogFlavourDescriptor
import java.io.File

data class Changelog(
    val file: File,
    val preTitle: String?,
    val title: String?,
    val introduction: String?,
    val unreleasedTerm: String,
    val headerParserRegex: Regex,
    val itemPrefix: String,
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

    private val preTitleNodes = tree.children
        .takeWhile { it.type != MarkdownElementTypes.ATX_1 }
        .takeIf { tree.children.any { it.type == MarkdownElementTypes.ATX_1 } }
        .orEmpty()
    val preTitleValue = preTitle ?: preTitleNodes
        .joinToString(lineSeparator) { it.text() }
        .trim()

    private val titleNodes = tree.children
        .dropWhile { it.type != MarkdownElementTypes.ATX_1 }
        .takeWhile { it.type == MarkdownElementTypes.ATX_1 }
    val titleValue = title ?: titleNodes
        .joinToString(lineSeparator) { it.text() }
        .trim()

    private val introductionNodes = tree.children
        .dropWhile { it.type != MarkdownElementTypes.ATX_1 }
        .dropWhile { it.type == MarkdownElementTypes.ATX_1 }
        .takeWhile { it.type != MarkdownElementTypes.ATX_2 }
    val introductionValue = introduction ?: introductionNodes
        .joinToString(lineSeparator) { it.text() }
        .reformat(lineSeparator)
        .trim()

    private val itemsNodes = tree.children
        .dropWhile { it.type != MarkdownElementTypes.ATX_2 }
    private val items = itemsNodes
        .groupByType(MarkdownElementTypes.ATX_2) {
            it.children.last().text().trim().run {
                when (this) {
                    unreleasedTerm -> this
                    else -> split("""[^-+.0-9a-zA-Z]+""".toRegex()).firstOrNull(
                        headerParserRegex::matches
                    ) ?: throw HeaderParseException(this, unreleasedTerm)
                }
            }
        }
        .filterKeys(String::isNotEmpty)
        .mapKeys {
            headerParserRegex.matchEntire(it.key)?.run {
                groupValues.drop(1).firstOrNull()
            } ?: it.key
        }
        .mapValues { (key, value) ->
            val header = value
                .firstOrNull { it.type == MarkdownElementTypes.ATX_2 }?.text()
                .orEmpty()
                .removePrefix("$ATX_2 ")
                .trim()

            val nodes = value
                .drop(1)
                .dropWhile { node -> node.type == MarkdownTokenTypes.EOL }

            val isUnreleased = key == unreleasedTerm
            val summaryNodes = nodes
                .takeWhile { node ->
                    node.type != MarkdownElementTypes.ATX_3 && !node.text().startsWith(itemPrefix)
                }
            val summary = summaryNodes
                .joinToString(lineSeparator) { it.text() }
                .reformat(lineSeparator)
                .trim()

            val items = nodes
                .drop(summaryNodes.size)
                .groupByType(MarkdownElementTypes.ATX_3) {
                    it.text().trimStart('#').trim()
                }
                .mapValues { section ->
                    section.value
                        .map { it.text().trim() }
                        .filterNot { it.startsWith(ATX_3) || it.isEmpty() }
                        .joinToString(lineSeparator)
                        .split("""(^|$lineSeparator)${Regex.escape(itemPrefix)}\s*""".toRegex())
                        .mapNotNull {
                            "$itemPrefix $it".takeIf { _ ->
                                it.isNotEmpty()
                            }
                        }
                        .toSet()

                }

            Item(key, header, summary, items, isUnreleased, lineSeparator)
        }

    fun has(version: String) = items.containsKey(version)

    fun get(version: String) = items[version] ?: throw MissingVersionException(version)

    fun getAll() = items

    fun getLatest() = items[items.keys.first()] ?: throw MissingVersionException("any")

    data class Item(
        var version: String,
        val header: String,
        val summary: String,
        private val items: Map<String, Set<String>> = emptyMap(),
        private val isUnreleased: Boolean = false,
        private val lineSeparator: String,
    ) {

        private var withHeader = true
        private var withSummary = true
        private var withEmptySections = isUnreleased
        private var filterCallback: ((String) -> Boolean)? = null

        fun withHeader(header: Boolean) = copy(withHeader = header)

        fun withSummary(summary: Boolean) = copy(withSummary = summary)

        fun withEmptySections(emptySections: Boolean) = copy(withEmptySections = emptySections)

        fun withFilter(filter: ((String) -> Boolean)?) = copy(filterCallback = filter)

        fun getSections() = items
            .mapValues { it.value.filter { item -> filterCallback?.invoke(item) ?: true } }
            .filter { it.value.isNotEmpty() || withEmptySections }

        fun toText() = sequence {
            if (withHeader) {
                yield("$ATX_2 $header")
            }

            if (withSummary && summary.isNotEmpty()) {
                yield(summary)
            }

            getSections()
                .entries
                .asSequence()
                .iterator()
                .run {
                    while (hasNext()) {
                        val (section, entries) = next()

                        if (section.isNotEmpty()) {
                            yield("$ATX_3 $section")
                        }

                        yieldAll(entries)
                    }
                }
        }
            .joinToString(lineSeparator)
            .reformat(lineSeparator)

        fun toHTML() = markdownToHTML(toText())

        fun toPlainText() = markdownToPlainText(toText(), lineSeparator)

        override fun toString() = toText()

        private fun copy(
            summary: String = this.summary,
            withHeader: Boolean = this.withHeader,
            withSummary: Boolean = this.withSummary,
            withEmptySections: Boolean = this.withEmptySections,
            filterCallback: ((String) -> Boolean)? = this.filterCallback,
        ) = Item(
            version,
            header,
            summary,
            items,
            isUnreleased,
            lineSeparator,
        ).also {
            it.withHeader = withHeader
            it.withSummary = withSummary
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
            )
        }

        operator fun Map<String, Set<String>>.plus(other: Map<String, Set<String>>): Map<String, Set<String>> {

            return this.mapValues { (key, value) ->
                value + other[key].orEmpty()
            }.toMutableMap().also { map ->
                map.putAll(other.filterKeys { !this.containsKey(it) })
            }
        }

        operator fun plus(items: List<Item>) = items.fold(this) { acc, item -> acc + item }
    }

    private fun ASTNode.text() = getTextInNode(content).toString()

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
}
