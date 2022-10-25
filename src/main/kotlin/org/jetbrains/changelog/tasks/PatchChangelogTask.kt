// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.ChangelogSectionUrlBuilder
import org.jetbrains.changelog.Version

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
    abstract val preTitle: Property<String>

    @get:Input
    @get:Optional
    abstract val title: Property<String>

    @get:Input
    @get:Optional
    abstract val introduction: Property<String>

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

    @get:Input
    @get:Optional
    abstract val version: Property<String>

    @get:Internal
    abstract val lineSeparator: Property<String>

    @get:Internal
    abstract val combinePreReleases: Property<Boolean>

    @get:Internal
    abstract val repositoryUrl: Property<String>

    @get:Internal
    abstract val sectionUrlBuilder: Property<ChangelogSectionUrlBuilder>

    @TaskAction
    fun run() {
        val unreleasedTermValue = unreleasedTerm.get()

        val changelog = Changelog(
            file = inputFile.get().asFile,
            defaultPreTitle = preTitle.orNull,
            defaultTitle = title.orNull,
            defaultIntroduction = introduction.orNull,
            unreleasedTerm = unreleasedTerm.get(),
            groups = groups.get(),
            headerParserRegex = headerParserRegex.get(),
            itemPrefix = itemPrefix.get(),
            repositoryUrl = repositoryUrl.orNull,
            sectionUrlBuilder = sectionUrlBuilder.get(),
            lineSeparator = lineSeparator.get(),
        )

        val releasedItems = changelog
            .getAll()
            .filterNot { it.key == unreleasedTermValue }
            .values

        val preReleaseItems = releasedItems
            .filter {
                val current = Version.parse(version.get())
                with(Version.parse(it.version)) {
                    current.major == major && current.minor == minor && current.patch == patch
                }
            }
            .takeIf { combinePreReleases.get() }
            .orEmpty()

        val item = Changelog.Item(
            version = version.get(),
            header = header.get(),
            itemPrefix = itemPrefix.get(),
            lineSeparator = lineSeparator.get(),
        ) + changelog.runCatching { get(unreleasedTermValue) }.getOrNull() + preReleaseItems

        val content = releaseNote ?: item
            .withEmptySections(false)
            .withHeader(false)
            .toText()

        if (patchEmpty.get() && content.isEmpty()) {
            logger.info(":patchChangelog task skipped due to the missing release note in the '$unreleasedTerm'.")
            throw StopActionException()
        }

//        if (item.getSections().isEmpty() && content.isBlank()) {
//            throw MissingReleaseNoteException(
//                ":patchChangelog task requires release note to be provided. " +
//                        "Add '$LEVEL_2 $unreleasedTermValue' section header to your changelog file: " +
//                        "'${inputFile.get().asFile.canonicalPath}' or provide it using '--release-note' CLI option."
//            )
//        }



//        val patchedContent = compose(
////            preTitle = preTitleValue,
////            title = titleValue,
////            introduction = introductionValue,
////            unreleasedItem = changelog.unreleasedItem.takeIf { keepUnreleasedSection.get() },
////            items = releasedItems,
////            repositoryUrl = repositoryUrl.orNull,
////            sectionUrlBuilder = sectionUrlBuilder.get(),
////            lineSeparator = lineSeparator.get(),
//            changelog,
//            sectionUrlBuilder.get(),
//            lineSeparator.get(),
//        ) {
//            yield("$LEVEL_2 ${item.header}")
//
//            if (content.isNotBlank()) {
//                yield(content)
//            } else {
//                yield(item.withHeader(false).toString())
//            }
//        }

        val patchedContent = changelog.render(sectionUrlBuilder.get())

        outputFile.get()
            .asFile
            .writeText(patchedContent)
    }
}
