// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.tasks

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.ChangelogPluginExtension

/**
 * Creates a new changelog file with an unreleased section and empty groups.
 */
abstract class InitializeChangelogTask : BaseChangelogTask() {

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
        val file = outputFile.get().asFile
        if (file.exists() && file.readText().isNotEmpty()) {
            throw GradleException("Changelog file is not empty: ${file.absolutePath}")
        }

        with (changelog.get()) {
            items = mapOf(
                unreleasedTerm.get() to newUnreleasedItem
            )

            render(Changelog.OutputType.MARKDOWN).let {
                file.writeText(it)
            }
        }
    }
}
