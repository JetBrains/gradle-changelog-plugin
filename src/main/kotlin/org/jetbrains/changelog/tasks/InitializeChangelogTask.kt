package org.jetbrains.changelog.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.jetbrains.changelog.ChangelogPluginExtension
import java.io.File

open class InitializeChangelogTask : DefaultTask() {

    private val extension = project.extensions.getByType(ChangelogPluginExtension::class.java)

    @TaskAction
    fun run() {
        val groups = extension.groups.get()
        File(extension.path.get()).apply {
            if (!exists()) {
                createNewFile()
            }
        }.writeText(
            """
                # Changelog
                
                ## ${extension.unreleasedTerm.get()}
                ${groups.firstOrNull()?.let { "### $it" } ?: ""}
                ${extension.itemPrefix.get()} Example item
                
                
            """.trimIndent() + groups.drop(1).joinToString("\n") { "### $it\n" }
        )
    }
}
