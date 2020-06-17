package org.jetbrains.changelog

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.GeneratingProvider
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.html.OpenCloseGeneratingProvider
import org.intellij.markdown.parser.LinkMap
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.changelog.exceptions.MissingFileException
import org.jetbrains.changelog.exceptions.MissingVersionException
import java.io.File
import java.net.URI

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

    private val items = tree.children
        .groupByType(MarkdownElementTypes.ATX_2) {
            it.children.last().text().trim().run {
                when (this) {
                    extension.unreleasedTerm -> this
                    else -> extension.headerMessageFormat().parse(this).first().toString()
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
                        .split("""\n${Regex.escape(extension.itemPrefix)}""".toRegex())
                        .map { line -> extension.itemPrefix + line.trim('\n') }
                        .drop(1)
                        .filterNot(String::isEmpty)
                }.run {
                    Item(key, value.first(), this)
                }
        }

    fun hasVersion(version: String) = items.containsKey(version)

    fun get(version: String) = items[version] ?: throw MissingVersionException(version)

    fun getLatest() = items[items.keys.first()] ?: throw MissingVersionException("any")

    inner class Item(val version: String, private val header: ASTNode, private val items: Map<String, List<String>>) {

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
            .filter {
                it.value.isNotEmpty()
            }

        fun toText() = getSections().entries
            .joinToString("\n\n") { (key, value) ->
                (listOfNotNull("### $key".takeIf { key.isNotEmpty() }) + value).joinToString("\n")
            }.let {
                when {
                    withHeader -> "${getHeader()}\n$it"
                    else -> it
                }
            }

        fun toHTML() = toText().run {
            HtmlGenerator(this, parser.buildMarkdownTreeFromString(this), flavour, false).generateHtml()
        }

        fun toPlainText() = toText().run {
            HtmlGenerator(this, parser.buildMarkdownTreeFromString(this), PlainTextFlavourDescriptor(), false)
                    .generateHtml(PlainTextTagRenderer())
        }

        override fun toString() = toText()
    }

    private class PlainTextTagRenderer : HtmlGenerator.TagRenderer {
        override fun openTag(
            node: ASTNode,
            tagName: CharSequence,
            vararg attributes: CharSequence?,
            autoClose: Boolean
        ) = ""
        override fun closeTag(tagName: CharSequence) = ""
        override fun printHtml(html: CharSequence) = html
    }

    private class PlainTextFlavourDescriptor : GFMFlavourDescriptor() {
        override fun createHtmlGeneratingProviders(linkMap: LinkMap, baseURI: URI?):
                Map<IElementType, GeneratingProvider> {

            return super.createHtmlGeneratingProviders(linkMap, baseURI) + hashMapOf(
                    MarkdownElementTypes.LIST_ITEM to CustomProvider("- "),
                    MarkdownTokenTypes.EOL to CustomProvider("", "\n")
            )
        }

        private class CustomProvider(private val openTagName: String = "", private val closeTagName: String = "") :
                OpenCloseGeneratingProvider() {

            override fun openTag(visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, node: ASTNode) {
                visitor.consumeHtml(openTagName)
            }

            override fun closeTag(visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, node: ASTNode) {
                visitor.consumeHtml(closeTagName)
            }
        }
    }

    private fun ASTNode.text() = getTextInNode(content).toString()

    private fun List<ASTNode>.groupByType(
        type: IElementType,
        getKey: ((item: ASTNode) -> String)? = null
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
