package org.jetbrains.changelog

import groovy.lang.Closure
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

fun <T : Any> closure(function: () -> T) = object : Closure<T>(null) {
    @Suppress("unused")
    fun doCall() = function()
}

fun markdownToHTML(input: String) = ChangelogFlavourDescriptor().run {
    HtmlGenerator(input, MarkdownParser(this).buildMarkdownTreeFromString(input), this, false).generateHtml()
}
