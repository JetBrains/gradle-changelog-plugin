// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.changelog.ChangelogPluginConstants.DATE_PATTERN
import org.jetbrains.changelog.flavours.ChangelogFlavourDescriptor
import org.jetbrains.changelog.flavours.PlainTextFlavourDescriptor
import java.text.SimpleDateFormat
import java.util.*

fun date(pattern: String = DATE_PATTERN) = SimpleDateFormat(pattern).format(Date())!!

fun markdownToHTML(input: String, lineSeparator: String = "\n") = ChangelogFlavourDescriptor().run {
    // Normalize text to LF, because a Markdown library currently fully supports only this line separator
    val lfString = input.normalizeLineSeparator("\n")
    HtmlGenerator(lfString, MarkdownParser(this).buildMarkdownTreeFromString(lfString), this, false)
        .generateHtml()
        .normalizeLineSeparator(lineSeparator)
}

fun markdownToPlainText(input: String, lineSeparator: String) = PlainTextFlavourDescriptor(lineSeparator).run {
    // Normalize text to LF, because a Markdown library currently fully supports only this line separator
    val lfString = input.normalizeLineSeparator("\n")
    HtmlGenerator(lfString, MarkdownParser(this).buildMarkdownTreeFromString(lfString), this, false)
        .generateHtml(PlainTextTagRenderer())
}

internal fun String.reformat(lineSeparator: String): String {
    val result = listOf(
        """(?:^|$lineSeparator)+(#+ [^$lineSeparator]*)(?:$lineSeparator)*""".toRegex() to "$lineSeparator$lineSeparator$1$lineSeparator$lineSeparator",
        """((?:^|$lineSeparator)#+ .*?)$lineSeparator(#+ )""".toRegex() to "$1$lineSeparator$lineSeparator$2",
        """(?:$lineSeparator)+(\[.*?]:)""".toRegex() to "$lineSeparator$lineSeparator$1",
        """(?<=$lineSeparator)(\[.*?]:.*?)(?:$lineSeparator)+""".toRegex() to "$1$lineSeparator",
        """(#+ .*?)$lineSeparator""".toRegex() to "$1$lineSeparator$lineSeparator",
        """($lineSeparator){3,}""".toRegex() to "$lineSeparator$lineSeparator",
    ).fold(this) { acc, (pattern, replacement) ->
        acc.replace(pattern, replacement)
    }.trim() + lineSeparator

    return when (result) {
        this -> result
        else -> result.reformat(lineSeparator)
    }
}

internal fun String.normalizeLineSeparator(lineSeparator: String) = replace("\\R".toRegex(), lineSeparator)

fun interface ChangelogSectionUrlBuilder {
    val extraParams  get() = emptyMap<String, String>()

    fun build(repositoryUrl: String, currentVersion: String?, previousVersion: String?, isUnreleased: Boolean): String
}
