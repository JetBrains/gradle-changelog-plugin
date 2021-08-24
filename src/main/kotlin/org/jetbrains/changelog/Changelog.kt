package org.jetbrains.changelog

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.changelog.exceptions.HeaderParseException
import org.jetbrains.changelog.exceptions.MissingFileException
import org.jetbrains.changelog.exceptions.MissingVersionException
import org.jetbrains.changelog.flavours.ChangelogFlavourDescriptor
import java.io.File

class Changelog(
    file: File,
    unreleasedTerm: String,
    headerParserRegex: Regex,
    itemPrefix: String,
) {

    val content = file.run {
        if (!exists()) {
            throw MissingFileException(canonicalPath)
        }
        readText()
    }

    private val flavour = ChangelogFlavourDescriptor()
    private val parser = MarkdownParser(flavour)
    private val tree = parser.buildMarkdownTreeFromString(content)

    private val items = tree.children
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
        .mapValues { (key, value) ->
            value
                .drop(1)
                .groupByType(MarkdownElementTypes.ATX_3) {
                    it.text().trimStart('#').trim()
                }
                .mapValues {
                    it.value
                        .joinToString("") { node -> node.text() }
                        .split("""\n${Regex.escape(itemPrefix)}""".toRegex())
                        .map { line -> itemPrefix + line.trim('\n') }
                        .drop(1)
                        .filterNot(String::isEmpty)
                }.run {
                    val isUnreleased = key == unreleasedTerm
                    Item(key, value.first(), this, isUnreleased)
                }
        }

    fun has(version: String) = items.containsKey(version)

    fun get(version: String) = items[version] ?: throw MissingVersionException(version)

    fun getLatest() = items[items.keys.first()] ?: throw MissingVersionException("any")

    fun getAll() = items

    inner class Item(
        val version: String,
        private val header: ASTNode,
        private val items: Map<String, List<String>>,
        private val isUnreleased: Boolean = false,
    ) {

        private var withHeader = false
        private var filterCallback: ((String) -> Boolean)? = null

        fun withHeader(header: Boolean) = apply {
            this.withHeader = header
        }

        fun withFilter(filter: ((String) -> Boolean)?) = apply {
            this.filterCallback = filter
        }

        fun getHeaderNode() = header

        fun getHeader() = header.text()

        fun getSections() = items
            .mapValues {
                it.value.filter { item -> filterCallback?.invoke(item) ?: true }
            }
            .filterNot {
                it.value.isEmpty() && !isUnreleased
            }

        fun toText() = getSections().entries
            .joinToString("\n\n") { (key, value) ->
                (listOfNotNull("### $key".takeIf { key.isNotEmpty() }) + value).joinToString("\n")
            }.trim().let {
                when {
                    withHeader -> "${getHeader()}\n$it"
                    else -> it
                }
            }

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
