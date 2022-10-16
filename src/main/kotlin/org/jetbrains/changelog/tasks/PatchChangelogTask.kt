// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.ChangelogPluginConstants.ATX_2
import org.jetbrains.changelog.ChangelogPluginConstants.ATX_3
import org.jetbrains.changelog.ChangelogPluginConstants.NEW_LINE
import org.jetbrains.changelog.exceptions.MissingReleaseNoteException

abstract class PatchChangelogTask : DefaultTask() {

    @get:Input
    @get:Optional
    @Option(option = "release-note", description = "Custom release note content")
    var releaseNote: String? = null

    @get:InputFile
    @get:Optional
    abstract val inputFile: RegularFileProperty

    @get:OutputFile
    @get:Optional
    abstract val outputFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val groups: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val header: Property<String>

    @get:Input
    @get:Optional
    abstract val headerParserRegex: Property<Regex>

    @get:Input
    @get:Optional
    abstract val itemPrefix: Property<String>

    @get:Input
    @get:Optional
    abstract val keepUnreleasedSection: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val patchEmpty: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val unreleasedTerm: Property<String>

    @TaskAction
    fun run() {
        val unreleasedTermValue = unreleasedTerm.get()
        val headerValue = header.get()
        val changelog = Changelog(
            inputFile.get().asFile,
            unreleasedTerm.get(),
            headerParserRegex.get(),
            itemPrefix.get(),
        )

        val item = changelog.runCatching { get(unreleasedTermValue) }.getOrNull()
        val noUnreleasedSection = item == null || item.getSections().isEmpty()
        val noReleaseNote = releaseNote.isNullOrBlank()
        val content = releaseNote ?: item?.withHeader(false)?.toText() ?: ""

        if (patchEmpty.get() && content.isEmpty()) {
            logger.info(":patchChangelog task skipped due to the missing release note in the '$unreleasedTerm'.")
            throw StopActionException()
        }

        if (noUnreleasedSection && noReleaseNote && content.isEmpty()) {
            throw MissingReleaseNoteException(
                ":patchChangelog task requires release note to be provided. " +
                    "Add '$ATX_2 $unreleasedTermValue' section header to your changelog file: " +
                    "'${inputFile.get().asFile.canonicalPath}' " +
                    "or provide it using '--release-note' CLI option."
            )
        }

        outputFile.get().asFile.writeText(listOfNotNull(
            changelog.header + NEW_LINE,

            changelog.description + NEW_LINE,

            item?.header.takeIf {
                keepUnreleasedSection.get()
            },

            groups.get().joinToString(NEW_LINE) {
                "$ATX_3 $it$NEW_LINE"
            },

            "$ATX_2 $headerValue",

            content.trim() + NEW_LINE,

            changelog.getAll()
                .values
                .drop(1)
                .joinToString("$NEW_LINE$NEW_LINE") {
                    it.withHeader(true).toText().trim()
                },
        ).joinToString(NEW_LINE))
    }
}
