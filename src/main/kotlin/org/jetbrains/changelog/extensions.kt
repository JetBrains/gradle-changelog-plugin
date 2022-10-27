// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

import org.jetbrains.changelog.ChangelogPluginConstants.DATE_PATTERN
import java.text.SimpleDateFormat
import java.util.*

fun date(pattern: String = DATE_PATTERN) = SimpleDateFormat(pattern).format(Date())!!

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

fun interface ChangelogSectionUrlBuilder {
    fun build(repositoryUrl: String, currentVersion: String?, previousVersion: String?, isUnreleased: Boolean): String
}
