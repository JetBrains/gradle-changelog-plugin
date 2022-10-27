// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.intellij.markdown.MarkdownElementTypes.ATX_2
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.Version
import org.jetbrains.changelog.exceptions.MissingReleaseNoteException

abstract class PatchChangelogTask : DefaultTask() {

    @get:Input
    @get:Optional
    @Option(option = "release-note", description = "Custom release note content")
    var releaseNote: String? = null

    @get:Input
    @get:Optional
    abstract val version: Property<String>

    @get:Internal
    abstract val header: Property<String>

    @get:Internal
    abstract val keepUnreleasedSection: Property<Boolean>

    @get:Internal
    abstract val patchEmpty: Property<Boolean>

    @get:Internal
    abstract val combinePreReleases: Property<Boolean>

    @get:Internal
    abstract val changelog: Property<Changelog>

    @get:InputFile
    @get:Optional
    abstract val inputFile: RegularFileProperty

    @get:Internal
    val content = inputFile.map {
        with(it.asFile) {
            if (!exists()) {
                createNewFile()
            }
            readText()
        }
    }

    @get:OutputFile
    @get:Optional
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun run() {
//        val item = Changelog.Item(
//            version = version.get(),
//            header = header.get(),
//            itemPrefix = itemPrefix.get(),
//            lineSeparator = lineSeparator.get(),
//        ) + changelog.runCatching { get(unreleasedTermValue) }.getOrNull() + preReleaseItems
//
//        val content = releaseNote ?: item
//            .withEmptySections(false)
//            .withHeader(false)
//            .toText()
//
//        if (patchEmpty.get() && content.isEmpty()) {
//            logger.info(":patchChangelog task skipped due to the missing release note in the '$unreleasedTerm'.")
//            throw StopActionException()
//        }

        with(changelog.get()) {
            val preReleaseItems = releasedItems
                .filter {
                    val current = Version.parse(version.get())
                    with(Version.parse(it.version)) {
                        current.major == major && current.minor == minor && current.patch == patch
                    }
                }
                .takeIf { combinePreReleases.get() }
                .orEmpty()

            val newItem = (unreleasedItem ?: newUnreleasedItem).copy(
                version = version.get(),
                header = header.get(),
                isUnreleased = false,
            ).let {
                parseTree(releaseNote)?.let { releaseNoteTree ->
                    val (summary, items) = releaseNoteTree.children.extractItemData(releaseNote)
                    val links = releaseNoteTree.children.extractLinks(releaseNote)

                    baseLinks.addAll(links)

                    it.copy(
                        summary = summary,
                        items = items,
                    )
                } ?: it
            } + preReleaseItems

            if (newItem.summary.isEmpty() && newItem.sections.all { it.value.isEmpty() }) {
                if (patchEmpty.get()) {
                    logger.info(":patchChangelog task skipped due to the missing release note in the '$unreleasedTerm'.")
                    throw StopActionException()
                }

                throw MissingReleaseNoteException(
                    ":patchChangelog task requires release note to be provided. " +
                            "Add '$ATX_2 $unreleasedTerm' section header with release notes to your changelog file: " +
                            "'${inputFile.get().asFile.canonicalPath}' or provide it using '--release-note' CLI option."
                )
            }

            items = sequence {
                if (keepUnreleasedSection.get()) {
                    yield(unreleasedTerm to newUnreleasedItem)
                }
                yield(newItem.version to newItem)
                releasedItems.forEach {
                    yield(it.version to it)
                }
            }.toMap()

            render(Changelog.OutputType.MARKDOWN).let {
                outputFile.get()
                    .asFile
                    .writeText(it)
            }
        }
    }
}
