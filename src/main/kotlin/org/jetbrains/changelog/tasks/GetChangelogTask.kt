// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.jetbrains.changelog.Changelog

abstract class GetChangelogTask : DefaultTask() {

    @get:Input
    @Option(option = "no-header", description = "Omits header version line")
    var noHeader = false

    @get:Input
    @Option(option = "no-summary", description = "Omits summary section")
    var noSummary = false

    @get:Input
    @get:Optional
    @Option(option = "version", description = "Returns change notes for the specified version")
    var cliVersion = null as String?

    @get:Input
    @Option(option = "unreleased", description = "Returns Unreleased change notes")
    var unreleased = false

    @get:InputFile
    @get:Optional
    abstract val inputFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val headerParserRegex: Property<Regex>

    @get:Input
    @get:Optional
    abstract val itemPrefix: Property<String>

    @get:Input
    @get:Optional
    abstract val unreleasedTerm: Property<String>

    @get:Input
    @get:Optional
    abstract val version: Property<String>

    @get:Internal
    abstract val groups: ListProperty<String>

    @get:Internal
    abstract val lineSeparator: Property<String>

    @get:Internal
    abstract val repositoryUrl: Property<String>

    @TaskAction
    fun run() = logger.quiet(
        Changelog(
            file = inputFile.map { it.asFile }.get(),
            defaultPreTitle = null,
            defaultTitle = null,
            defaultIntroduction = null,
            unreleasedTerm = unreleasedTerm.get(),
            groups = emptyList(),
            headerParserRegex = headerParserRegex.get(),
            itemPrefix = itemPrefix.get(),
            repositoryUrl = repositoryUrl.orNull,
            sectionUrlBuilder = sectionUrlBuilder.get(),
            lineSeparator = lineSeparator.get(),
        ).let {
            val version = cliVersion ?: when (unreleased) {
                true -> unreleasedTerm
                false -> version
            }.get()

            it
                .get(version)
                .withHeader(!noHeader)
                .withSummary(!noSummary)
                .toText()
        }
    )
}
