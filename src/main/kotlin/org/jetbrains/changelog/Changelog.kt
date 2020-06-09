package org.jetbrains.changelog

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.changelog.exceptions.MissingFileException
import org.jetbrains.changelog.exceptions.MissingVersionException
import java.io.File

class Changelog(extension: ChangelogPluginExtension) {
    val content = File(extension.path).run {
        if (extension.path.isEmpty() || !exists()) {
            throw MissingFileException(extension.path)
        }
        readText()
    }
    private val flavour = ChangelogFlavourDescriptor()
    private val parser = MarkdownParser(flavour)
    private val tree = parser.buildMarkdownTreeFromString(content)

    private val items = tree.children.groupByType(MarkdownElementTypes.ATX_2) {
        extension.headerFormat().parse(it.text()).first().toString()
    }.filterKeys(String::isNotEmpty).mapValues {
        val header = it.value.first()
        val items = it.value.drop(1).groupByType(MarkdownElementTypes.ATX_3) { key ->
            key.text().trimStart('#').trim()
        }.filterKeys(String::isNotEmpty).mapValues {
            it.value.joinToString("") { it.text() }.lines().drop(1).filterNot(String::isEmpty)
        }
        Item(it.key, header, items)
    }

    fun hasVersion(version: String) = items.containsKey(version)

    fun get(version: String) = items[version] ?: throw MissingVersionException(version)

    fun getLatest() = items[items.keys.first()] ?: throw MissingVersionException("any")

    inner class Item(val version: String, private val header: ASTNode, private val items: Map<String, List<String>>) {
        private var withHeader = false
        private var filterCallback: ((String) -> Boolean)? = null

        fun withHeader(header: Boolean) = apply { this.withHeader = header }

        fun withFilter(filter: ((String) -> Boolean)?) = apply { this.filterCallback = filter; }

        fun getHeaderNode() = header

        fun getHeader() = header.text()

        fun getSections() = items.mapValues {
            it.value.filter { item -> filterCallback?.invoke(item) ?: true }
        }.filter { it.value.isNotEmpty() }

        fun toText() = getSections().entries
                .joinToString("\n\n") {
                    (listOf("### ${it.key}") + it.value).joinToString("\n")
                }.let {
                    when {
                        withHeader -> "${getHeader()}\n$it"
                        else -> it
                    }
                }

        fun toHTML() = toText().run {
            HtmlGenerator(this, parser.buildMarkdownTreeFromString(this), flavour, false).generateHtml()
        }

        override fun toString() = toText()
    }

    private fun ASTNode.text() = getTextInNode(content).toString()

    private fun List<ASTNode>.groupByType(type: IElementType, getKey: ((item: ASTNode) -> String)? = null): Map<String, List<ASTNode>> {
        var key = ""
        return groupBy {
            if (it.type == type) {
                key = getKey?.invoke(it) ?: it.text()
            }
            key
        }
    }
}
