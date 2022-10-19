// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.changelog.ChangelogPluginConstants.ATX_1
import org.jetbrains.changelog.ChangelogPluginConstants.ATX_2
import org.jetbrains.changelog.ChangelogPluginConstants.ATX_3
import org.jetbrains.changelog.ChangelogPluginConstants.NEW_LINE
import org.jetbrains.changelog.flavours.ChangelogFlavourDescriptor
import org.jetbrains.changelog.flavours.PlainTextFlavourDescriptor
import java.text.SimpleDateFormat
import java.util.*

fun date(pattern: String = ChangelogPluginConstants.DATE_PATTERN) = SimpleDateFormat(pattern).format(Date())!!

fun markdownToHTML(input: String) = ChangelogFlavourDescriptor().run {
    HtmlGenerator(input, MarkdownParser(this).buildMarkdownTreeFromString(input), this, false)
        .generateHtml()
}

fun markdownToPlainText(input: String) = PlainTextFlavourDescriptor().run {
    HtmlGenerator(input, MarkdownParser(this).buildMarkdownTreeFromString(input), this, false)
        .generateHtml(PlainTextTagRenderer())
}

fun String.reformat(): String {
    val result = listOf(
        """(?:^|\n)+(#+ [^\n]*)\n*""".toRegex() to "\n\n$1\n",
        """((?:^|\n)#+ .*?)\n(#+ )""".toRegex() to "$1\n\n$2",
        """\n{3,}""".toRegex() to "\n\n",
    ).fold(this) { acc, (pattern, replacement) ->
        acc.replace(pattern, replacement)
    }.trim() + NEW_LINE

    return when (result) {
        this -> result
        else -> result.reformat()
    }

}

internal fun compose(
    preTitle: String?,
    title: String?,
    introduction: String?,
    unreleasedTerm: String?,
    groups: List<String>,
    function: suspend SequenceScope<String>.() -> Unit = {},
) = sequence {
    if (!preTitle.isNullOrBlank()) {
        yield(preTitle)
        yield(NEW_LINE)
    }
    if (!title.isNullOrBlank()) {
        yield("$ATX_1 ${title.trim()}")
        yield(NEW_LINE)
    }
    if (!introduction.isNullOrBlank()) {
        yield(introduction)
        yield(NEW_LINE)
    }

    if (!unreleasedTerm.isNullOrBlank()) {
        yield("$ATX_2 $unreleasedTerm")

        groups
            .map { "$ATX_3 $it" }
            .let { yieldAll(it) }
    }

    function()
}
    .joinToString(NEW_LINE)
    .reformat()
