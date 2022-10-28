// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.tasks

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.intellij.markdown.MarkdownElementTypes.ATX_2
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.ChangelogPluginExtension
import org.jetbrains.changelog.Version
import org.jetbrains.changelog.exceptions.MissingReleaseNoteException

/**
 * Updates the unreleased section to the given version.
 */
abstract class PatchChangelogTask : BaseChangelogTask() {

    /**
     * Use custom release note to create new changelog entry.
     */
    @get:Input
    @get:Optional
    @Option(option = "release-note", description = "Custom release note content")
    var releaseNote: String? = null

    /**
     * @see [ChangelogPluginExtension.version]
     */
    @get:Internal
    abstract val version: Property<String>

    /**
     * @see [ChangelogPluginExtension.header]
     */
    @get:Internal
    abstract val header: Property<String>

    /**
     * @see [ChangelogPluginExtension.keepUnreleasedSection]
     */
    @get:Internal
    abstract val keepUnreleasedSection: Property<Boolean>

    /**
     * @see [ChangelogPluginExtension.patchEmpty]
     */
    @get:Internal
    abstract val patchEmpty: Property<Boolean>

    /**
     * @see [ChangelogPluginExtension.combinePreReleases]
     */
    @get:Internal
    abstract val combinePreReleases: Property<Boolean>

    /**
     * Changelog input file.
     *
     * Default value: file resolved with [ChangelogPluginExtension.path]
     */
    @get:InputFile
    @get:Optional
    abstract val inputFile: RegularFileProperty

    /**
     * Changelog output file.
     *
     * Default value: file resolved with [ChangelogPluginExtension.path]
     */
    @get:OutputFile
    @get:Optional
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun run() {
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
                    logger.info(":patchChangelog task skipped due to the missing release note in the '${unreleasedTerm.get()}'.")
                    throw StopActionException()
                }

                throw MissingReleaseNoteException(
                    ":patchChangelog task requires release note to be provided. " +
                            "Add '$ATX_2 ${unreleasedTerm.get()}' section header with release notes to your changelog file: " +
                            "'${inputFile.get().asFile.canonicalPath}' or provide it using '--release-note' CLI option."
                )
            }

            items = sequence {
                if (keepUnreleasedSection.get()) {
                    yield(unreleasedTerm.get() to newUnreleasedItem)
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
