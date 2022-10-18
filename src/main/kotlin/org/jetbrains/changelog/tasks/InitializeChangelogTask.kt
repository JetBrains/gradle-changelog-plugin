// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.changelog.ChangelogPluginConstants.ATX_1
import org.jetbrains.changelog.ChangelogPluginConstants.ATX_2
import org.jetbrains.changelog.ChangelogPluginConstants.ATX_3
import org.jetbrains.changelog.ChangelogPluginConstants.NEW_LINE
import org.jetbrains.changelog.reformat

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

        sequence {
            if (preTitle.isPresent) {
                yield(preTitle.get())
                yield(NEW_LINE)
            }
            if (title.isPresent) {
                yield("$ATX_1 ${title.get()}")
                yield(NEW_LINE)
            }
            if (introduction.isPresent) {
                yield(introduction.get())
                yield(NEW_LINE)
            }

            yield("$ATX_2 ${unreleasedTerm.get()}")

            groups.get()
                .map { "$ATX_3 $it" }
                .let { yieldAll(it) }
        }
            .joinToString(NEW_LINE)
            .reformat()
            .let {
                file.writeText(it)
            }
    }
}
