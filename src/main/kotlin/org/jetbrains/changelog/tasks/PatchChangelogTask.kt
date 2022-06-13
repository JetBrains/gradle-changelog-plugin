// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.StopActionException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.ChangelogPluginConstants.ATX_2
import org.jetbrains.changelog.ChangelogPluginConstants.ATX_3
import org.jetbrains.changelog.ChangelogPluginConstants.NEW_LINE
import org.jetbrains.changelog.exceptions.MissingReleaseNoteException
import javax.inject.Inject

open class PatchChangelogTask @Inject constructor(
    objectFactory: ObjectFactory,
) : DefaultTask() {

    @Input
    @Option(option = "release-note", description = "Custom release note content")
    @Optional
    var releaseNote: String? = null

    @InputFile
    @Optional
    val inputFile = objectFactory.fileProperty()

    @OutputFile
    @Optional
    val outputFile = objectFactory.fileProperty()

    @Input
    @Optional
    val groups = objectFactory.listProperty<String>()

    @Input
    @Optional
    val header = objectFactory.property<String>()

    @Input
    @Optional
    val headerParserRegex = objectFactory.property<Regex>()

    @Input
    @Optional
    val itemPrefix = objectFactory.property<String>()

    @Input
    @Optional
    val keepUnreleasedSection = objectFactory.property<Boolean>()

    @Input
    @Optional
    val patchEmpty = objectFactory.property<Boolean>()

    @Input
    @Optional
    val unreleasedTerm = objectFactory.property<String>()

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
