// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.changelog.compose

abstract class InitializeChangelogTask : DefaultTask() {

    @get:OutputFile
    @get:Optional
    abstract val outputFile: RegularFileProperty

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
    abstract val groups: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val unreleasedTerm: Property<String>

    @get:Internal
    abstract val lineSeparator: Property<String>

    @TaskAction
    fun run() {
        val file = outputFile.get()
            .asFile
            .apply {
                if (!exists()) {
                    createNewFile()
                }
            }

        if (file.readText().isNotEmpty()) {
            throw GradleException("Changelog file is not empty: ${file.absolutePath}")
        }

        val content = compose(
            preTitle = preTitle.orNull,
            title = title.orNull,
            introduction = introduction.orNull,
            unreleasedTerm = unreleasedTerm.get(),
            groups = groups.get(),
            lineSeparator = lineSeparator.get(),
        )
        file.writeText(content)
    }
}
