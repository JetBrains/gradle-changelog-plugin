// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.tasks

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.ChangelogPluginExtension
import org.jetbrains.changelog.exceptions.MissingVersionException
import java.security.Provider

/**
 * Retrieves changelog for the specified version.
 */
abstract class GetChangelogTask : BaseChangelogTask() {

    /**
     * Omits the section header in the changelog output.
     *
     * Default value: `false`
     */
    @get:Input
    @Option(
        option = "no-header",
        description = "Omits the section header in the changelog output.",
    )
    var noHeader = false

    /**
     * Omits the section summary in the changelog output.
     *
     * Default value: `false`
     */
    @get:Input
    @Option(
        option = "no-summary",
        description = "Omits the section summary in the changelog output.",
    )
    var noSummary = false

    /**
     * Omits links in the changelog output.
     *
     * Default value: `false`
     */
    @get:Input
    @Option(
        option = "no-links",
        description = "Omits links in the changelog output.",
    )
    var noLinks = false

    /**
     * Omits empty sections in the changelog output.
     *
     * Default value: `false`
     */
    @get:Input
    @Option(
        option = "no-empty-sections",
        description = "Omits empty sections in the changelog output.",
    )
    var noEmptySections = false

    /**
     * Returns change notes for the specified version.
     *
     * Default value: `null`
     */
    @get:Input
    @get:Optional
    @Option(
        option = "project-version",
        description = "Returns change notes for the specified project version.",
    )
    var projectVersion = null as String?

    /**
     * Returns change notes for an unreleased section.
     *
     * Default value: `false`
     */
    @get:Input
    @Option(
        option = "unreleased",
        description = "Returns change notes for an unreleased section.",
    )
    var unreleased = false

    /**
     * Changelog input file.
     *
     * Default value: file resolved with [ChangelogPluginExtension.path]
     */
    @get:InputFile
    @get:Optional
    abstract val inputFile: RegularFileProperty

    /**
     * Output file to write the changelog content to.
     *
     * Default value: `null`
     */
    @get:OutputFile
    @get:Optional
    abstract val outputFile: RegularFileProperty

    /**
     * Output file to write the changelog content to.
     *
     * Default value: `null`
     */
    @get:Optional
    @get:Input
    @Option(
        option = "output-file",
        description = "File to write the changelog content to.",
    )
    var outputFilePath = null as String?

    @TaskAction
    fun run() {
        val content = with(changelog.get()) {
            val version = projectVersion

            when {
                version != null -> get(version)
                unreleased -> unreleasedItem ?: throw MissingVersionException(unreleasedTerm.get())
                else -> releasedItems.firstOrNull() ?: throw MissingVersionException("any")
            }
                .withHeader(!noHeader)
                .withSummary(!noSummary)
                .withLinks(!noLinks)
                .withLinkedHeader(!noLinks)
                .withEmptySections(!noEmptySections)
                .let { renderItem(it, Changelog.OutputType.MARKDOWN) }
        }

        // Write to file if specified or output to logger
        outputFile.orNull?.let {
            it.asFile.apply {
                parentFile?.mkdirs()
                writeText(content)
            }
            logger.lifecycle("Changelog written to: $path")
        } ?: logger.quiet(content)
    }
}
