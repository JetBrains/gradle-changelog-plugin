package org.jetbrains.changelog

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
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
    private val flavour = GFMFlavourDescriptor()
    private val parser = MarkdownParser(flavour)
    private val tree = parser.buildMarkdownTreeFromString(content)

    private val items = tree.run {
        var key = ""
        children.groupBy {
            if (it.type == MarkdownElementTypes.ATX_2) {
                key = extension.headerFormat().parse(it.getTextInNode(content).toString()).first() as String
            }
            key
        }.filterKeys(String::isNotEmpty).mapValues { Item(it.key, it.value) }
    }

    @Suppress("unused")
    fun hasVersion(version: String) = getKey(version) != null

    fun get(version: String) = items[getKey(version)] ?: throw MissingVersionException(version)

    @Suppress("unused")
    fun getLatest() = items[items.keys.first()]

    private fun getKey(version: String) = items.keys.find { it.contains(version) }

    inner class Item(val version: String, private val nodes: List<ASTNode>) {
        private var header = false
        private var filterCallback: ((String) -> Boolean)? = null

        fun withHeader(header: Boolean) = apply { this.header = header }

        fun noHeader(noHeader: Boolean) = apply { this.noHeader = noHeader }

        fun getHeaderNode() = nodes.first()

        fun toText() = nodes.run {
            when {
                header -> drop(1)
                else -> this
            }
        }.filter {
            filterCallback?.invoke(it.getTextInNode(content).toString()) ?: true
        }.joinToString("") {
            it.getTextInNode(content)
        }.trim()

        fun toHTML() = toText().run {
            HtmlGenerator(this, parser.buildMarkdownTreeFromString(this), flavour, false).generateHtml()
        }

        override fun toString() = toText()
    }
}
