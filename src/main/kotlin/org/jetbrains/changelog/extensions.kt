// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.changelog.ChangelogPluginConstants.ATX_1
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

internal fun String.reformat() = this
    .replace("""$NEW_LINE{3,}""".toRegex(), NEW_LINE + NEW_LINE)
    .replace("""($NEW_LINE$ATX_1.*?)$NEW_LINE+""".toRegex(), "$1$NEW_LINE")
    .replace("""($NEW_LINE$ATX_1.*?)$NEW_LINE+""".toRegex(), "$1$NEW_LINE")
    .replace("""($ATX_1+ .*?$NEW_LINE)$ATX_1""".toRegex(), "$1$NEW_LINE$ATX_1")
    .trim() + NEW_LINE
