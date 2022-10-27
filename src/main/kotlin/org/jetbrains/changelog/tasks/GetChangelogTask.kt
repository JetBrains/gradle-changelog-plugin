// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.exceptions.MissingVersionException

abstract class GetChangelogTask : DefaultTask() {

    @get:Input
    @Option(option = "no-header", description = "Omits header version line")
    var noHeader = false

    @get:Input
    @Option(option = "no-summary", description = "Omits summary section")
    var noSummary = false

    @get:Input
    @Option(option = "no-links", description = "Omits links")
    var noLinks = false

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

    @get:Internal
    val content = inputFile.map {
        with(it.asFile) {
            if (!exists()) {
                createNewFile()
            }
            readText()
        }
    }

    @get:Internal
    abstract val changelog: Property<Changelog>

    @TaskAction
    fun run() = logger.quiet(
        with(changelog.get()) {
            val version = cliVersion

            when {
                version != null -> get(version)
                unreleased -> unreleasedItem ?: throw MissingVersionException(unreleasedTerm)
                else -> releasedItems.first()
            }
                .withHeader(!noHeader)
                .withSummary(!noSummary)
                .withLinks(!noLinks)
                .withLinkedHeader(!noLinks)
                .let {
                    renderItem(it, Changelog.OutputType.MARKDOWN)
                }
        }
    )
}
