package org.jetbrains.changelog.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

open class InitializeChangelogTask @Inject constructor(
    objectFactory: ObjectFactory,
) : DefaultTask() {

    @OutputFile
    @Optional
    val outputFile: RegularFileProperty = objectFactory.fileProperty()

    @Input
    @Optional
    val groups: ListProperty<String> = objectFactory.listProperty(String::class.java)

    @Input
    @Optional
    val unreleasedTerm: Property<String> = objectFactory.property(String::class.java)

    @Input
    @Optional
    val itemPrefix: Property<String> = objectFactory.property(String::class.java)

    @TaskAction
    fun run() {
        val groups = groups.get()
        outputFile.get().asFile.apply {
            if (!exists()) {
                createNewFile()
            }
        }.writeText(
            """
                # Changelog
                
                ## ${unreleasedTerm.get()}
                ${groups.firstOrNull()?.let { "### $it" } ?: ""}
                ${itemPrefix.get()} Example item
                
                
            """.trimIndent() + groups.drop(1).joinToString("\n") { "### $it\n" }
        )
    }
}
