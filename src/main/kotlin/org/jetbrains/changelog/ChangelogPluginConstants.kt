// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

object ChangelogPluginConstants {
    const val GROUP_NAME = "changelog"
    const val EXTENSION_NAME = "changelog"
    const val PLUGIN_NAME = "Gradle Changelog Plugin"
    const val MINIMAL_SUPPORTED_GRADLE_VERSION = "6.8"

    const val GET_CHANGELOG_TASK_NAME = "getChangelog"
    const val PATCH_CHANGELOG_TASK_NAME = "patchChangelog"
    const val INITIALIZE_CHANGELOG_TASK_NAME = "initializeChangelog"

    const val CHANGELOG_FILE_NAME = "CHANGELOG.md"
    const val DATE_PATTERN = "yyyy-MM-dd"
    const val ITEM_PREFIX = "-"
    const val INITIALIZE_HEADER = "Changelog"
    const val INITIALIZE_EXAMPLE_ITEM = "Example item"
    const val UNRELEASED_TERM = "[Unreleased]"
    const val NEW_LINE = "\n"
    const val ATX_1 = "#"
    const val ATX_2 = "##"
    const val ATX_3 = "###"

    private const val GROUP_ADDED = "Added"
    private const val GROUP_CHANGED = "Changed"
    private const val GROUP_DEPRECATED = "Deprecated"
    private const val GROUP_REMOVED = "Removed"
    private const val GROUP_FIXED = "Fixed"
    private const val GROUP_SECURITY = "Security"

    val GROUPS = listOf(
        GROUP_ADDED,
        GROUP_CHANGED,
        GROUP_DEPRECATED,
        GROUP_REMOVED,
        GROUP_FIXED,
        GROUP_SECURITY,
    )

    val SEM_VER_REGEX =
        """^((0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?)${'$'}""".toRegex() // ktlint-disable max-line-length
}
