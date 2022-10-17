// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.changelog.ChangelogPluginConstants.ATX_3
import org.jetbrains.changelog.ChangelogPluginConstants.NEW_LINE
import org.jetbrains.changelog.exceptions.HeaderParseException
import org.jetbrains.changelog.exceptions.MissingFileException
import org.jetbrains.changelog.exceptions.MissingVersionException
import org.jetbrains.changelog.flavours.ChangelogFlavourDescriptor
import java.io.File

class Changelog(
    file: File,
    introduction: String?,
    unreleasedTerm: String,
    headerParserRegex: Regex,
    itemPrefix: String,
) {

    private val content = file.run {
        if (!exists()) {
            throw MissingFileException(canonicalPath)
        }
        readText()
    }

    private val flavour = ChangelogFlavourDescriptor()
    private val parser = MarkdownParser(flavour)
    private val tree = parser.buildMarkdownTreeFromString(content)

    private val headerNodes = tree.children
        .takeWhile { it.type == MarkdownElementTypes.ATX_1 }
    val header = headerNodes
        .joinToString(NEW_LINE) { it.text() }
        .trim()

    private val introductionNodes = tree.children
        .dropWhile { it.type == MarkdownElementTypes.ATX_1 }
        .takeWhile { it.type != MarkdownElementTypes.ATX_2 }
    val introduction = introduction ?: introductionNodes
        .joinToString(NEW_LINE) { it.text() }
        .replace("""$NEW_LINE{3,}""".toRegex(), NEW_LINE + NEW_LINE)
        .trim()

    private val itemsNodes = tree.children
        .drop(headerNodes.size + introductionNodes.size)
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
                .joinToString(NEW_LINE) { it.text() }
                .replace("""$NEW_LINE{3,}""".toRegex(), NEW_LINE + NEW_LINE)
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
                        .joinToString(NEW_LINE)
                        .split("""(^|$NEW_LINE)${Regex.escape(itemPrefix)}\s*""".toRegex())
                        .mapNotNull {
                            "$itemPrefix $it".takeIf { _ ->
                                it.isNotEmpty()
                            }
                        }

                }

            Item(key, header, summary, items, isUnreleased)
        }

    fun has(version: String) = items.containsKey(version)

    fun get(version: String) = items[version] ?: throw MissingVersionException(version)

    fun getAll() = items

    fun getLatest() = items[items.keys.first()] ?: throw MissingVersionException("any")

    inner class Item(
        val version: String,
        val header: String,
        val summary: String,
        private val items: Map<String, List<String>>,
        private val isUnreleased: Boolean = false,
    ) {

        private var withHeader = false
        private var withSummary = true
        private var filterCallback: ((String) -> Boolean)? = null

        fun withHeader(header: Boolean) = apply {
            withHeader = header
        }

        fun withSummary(summary: Boolean) = apply {
            withSummary = summary
        }

        fun withFilter(filter: ((String) -> Boolean)?) = apply {
            filterCallback = filter
        }

        fun getSections() = items
            .mapValues { it.value.filter { item -> filterCallback?.invoke(item) ?: true } }
            .filterNot { it.value.isEmpty() && !isUnreleased }

        fun toText() = sequence {
            val hasSummary = withSummary && summary.isNotEmpty()

            if (withHeader) {
                yield(header)
            }
            if (hasSummary) {
                yield(summary)
            }

            if (withHeader || hasSummary) {
                yield(NEW_LINE)
            }

            getSections()
                .entries
                .asSequence()
                .iterator()
                .run {
                    while (hasNext()) {
                        val (section, items) = next()
                        if (section.isNotEmpty()) {
                            yield("$ATX_3 $section")
                        }
                        yieldAll(items)

                        if (hasNext()) {
                            yield(NEW_LINE)
                        }
                    }
                }
        }
            .joinToString(NEW_LINE)
            .replace("""$NEW_LINE{3,}""".toRegex(), NEW_LINE + NEW_LINE)
            .trim()

        fun toHTML() = markdownToHTML(toText())

        fun toPlainText() = markdownToPlainText(toText())

        override fun toString() = toText()
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
