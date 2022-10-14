// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.changelog.ChangelogPluginConstants
import org.jetbrains.changelog.ChangelogPluginConstants.ATX_1
import org.jetbrains.changelog.ChangelogPluginConstants.ATX_2
import org.jetbrains.changelog.ChangelogPluginConstants.ATX_3
import org.jetbrains.changelog.ChangelogPluginConstants.NEW_LINE

abstract class InitializeChangelogTask : DefaultTask() {

    @get:OutputFile
    @get:Optional
    abstract val outputFile: RegularFileProperty

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
        val groups = groups.get()
        outputFile.get().asFile.apply {
            if (!exists()) {
                createNewFile()
            }
        }.writeText(
            """
                $ATX_1 ${ChangelogPluginConstants.INITIALIZE_HEADER}
                
                $ATX_2 ${unreleasedTerm.get()}
                ${groups.firstOrNull()?.let { "$ATX_3 $it" } ?: ""}
                ${itemPrefix.get()} ${ChangelogPluginConstants.INITIALIZE_EXAMPLE_ITEM}
                
                
            """.trimIndent() + groups.drop(1).joinToString(NEW_LINE) { "$ATX_3 $it$NEW_LINE" }
        )
    }
}
