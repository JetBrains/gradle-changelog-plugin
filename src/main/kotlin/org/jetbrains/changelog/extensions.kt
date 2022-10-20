// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.changelog.ChangelogPluginConstants.ATX_1
import org.jetbrains.changelog.ChangelogPluginConstants.ATX_2
import org.jetbrains.changelog.ChangelogPluginConstants.ATX_3
import org.jetbrains.changelog.flavours.ChangelogFlavourDescriptor
import org.jetbrains.changelog.flavours.PlainTextFlavourDescriptor
import java.text.SimpleDateFormat
import java.util.*

fun date(pattern: String = ChangelogPluginConstants.DATE_PATTERN) = SimpleDateFormat(pattern).format(Date())!!

fun markdownToHTML(input: String) = ChangelogFlavourDescriptor().run {
    HtmlGenerator(input, MarkdownParser(this).buildMarkdownTreeFromString(input), this, false)
        .generateHtml()
}

fun markdownToPlainText(input: String, lineSeparator: String) = PlainTextFlavourDescriptor(lineSeparator).run {
    HtmlGenerator(input, MarkdownParser(this).buildMarkdownTreeFromString(input), this, false)
        .generateHtml(PlainTextTagRenderer())
}

fun String.reformat(lineSeparator: String): String {
    val result = listOf(
        """(?:^|$lineSeparator)+(#+ [^$lineSeparator]*)(?:$lineSeparator)*""".toRegex() to "$lineSeparator$lineSeparator$1$lineSeparator",
        """((?:^|$lineSeparator)#+ .*?)$lineSeparator(#+ )""".toRegex() to "$1$lineSeparator$lineSeparator$2",
        """($lineSeparator){3,}""".toRegex() to "$lineSeparator$lineSeparator",
    ).fold(this) { acc, (pattern, replacement) ->
        acc.replace(pattern, replacement)
    }.trim() + lineSeparator

    return when (result) {
        this -> result
        else -> result.reformat(lineSeparator)
    }
}

internal fun compose(
    preTitle: String?,
    title: String?,
    introduction: String?,
    unreleasedTerm: String?,
    groups: List<String>,
    lineSeparator: String,
    function: suspend SequenceScope<String>.() -> Unit = {},
) = sequence {
    if (!preTitle.isNullOrBlank()) {
        yield(preTitle)
    }
    if (!title.isNullOrBlank()) {
        yield("$ATX_1 $title")
    }
    if (!introduction.isNullOrBlank()) {
        yield(introduction)
    }

    if (!unreleasedTerm.isNullOrBlank()) {
        yield("$ATX_2 $unreleasedTerm")

        groups
            .map { "$ATX_3 $it" }
            .let { yieldAll(it) }
    }

    function()
}
    .joinToString(lineSeparator)
    .reformat(lineSeparator)
