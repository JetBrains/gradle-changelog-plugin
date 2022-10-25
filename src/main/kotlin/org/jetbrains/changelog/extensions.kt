// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.changelog.ChangelogPluginConstants.DATE_PATTERN
import org.jetbrains.changelog.ChangelogPluginConstants.LEVEL_1
import org.jetbrains.changelog.flavours.ChangelogFlavourDescriptor
import org.jetbrains.changelog.flavours.PlainTextFlavourDescriptor
import java.text.SimpleDateFormat
import java.util.*

fun date(pattern: String = DATE_PATTERN) = SimpleDateFormat(pattern).format(Date())!!

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
        """$lineSeparator+(\[.*?]:)""".toRegex() to "$lineSeparator$lineSeparator$1",
        """(?<=$lineSeparator)(\[.*?]:.*?)$lineSeparator+""".toRegex() to "$1$lineSeparator",
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
    unreleasedItem: Changelog.Item?,
    items: Collection<Changelog.Item>,
    repositoryUrl: String?,
    sectionUrlBuilder: ChangelogSectionUrlBuilder,
    lineSeparator: String,
    function: suspend SequenceScope<String>.() -> Unit = {},
) = sequence {
    if (!preTitle.isNullOrBlank()) {
        yield(preTitle)
    }
    if (!title.isNullOrBlank()) {
        yield("$LEVEL_1 $title")
    }
    if (!introduction.isNullOrBlank()) {
        yield(introduction)
    }

    unreleasedItem?.let {
        yield(it.withLinks(false).withEmptySections(true))
    }

    function()

    items.forEach {
        yield(it.withLinks(false))
    }

    repositoryUrl?.let {
        val build = sectionUrlBuilder::build

        unreleasedItem?.let {
            val url = build(repositoryUrl, it.version, items.first().version, true)
            yield("[${it.version}]: $url")
        }

        items.windowed(2).forEach { (current, previous) ->
            val url = build(repositoryUrl, current.version, previous.version, false)
            yield("[${current.version}]: $url")
        }

        items.last().let {
            val url = build(repositoryUrl, it.version, null, false)
            yield("[${it.version}]: $url")
        }
    }
}
    .joinToString(lineSeparator)
    .reformat(lineSeparator)

fun interface ChangelogSectionUrlBuilder {
    fun build(repositoryUrl: String, currentVersion: String?, previousVersion: String?, isUnreleased: Boolean): String
}
