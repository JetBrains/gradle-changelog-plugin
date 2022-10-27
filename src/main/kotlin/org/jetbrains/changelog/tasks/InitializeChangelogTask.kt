// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.changelog.Changelog

abstract class InitializeChangelogTask : DefaultTask() {

    @get:OutputFile
    @get:Optional
    abstract val outputFile: RegularFileProperty

    @get:Internal
    abstract val changelog: Property<Changelog>

    @TaskAction
    fun run() {
        val file = outputFile.get().asFile
        if (file.exists() && file.readText().isNotEmpty()) {
            throw GradleException("Changelog file is not empty: ${file.absolutePath}")
        }

        with (changelog.get()) {
            items = mapOf(
                unreleasedTerm to newUnreleasedItem
            )

            render(Changelog.OutputType.MARKDOWN).let {
                file.writeText(it)
            }
        }
    }
}
