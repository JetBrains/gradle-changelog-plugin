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
import org.jetbrains.changelog.ChangelogPluginConstants
import org.jetbrains.changelog.ChangelogPluginConstants.ATX_1
import org.jetbrains.changelog.ChangelogPluginConstants.ATX_2
import org.jetbrains.changelog.ChangelogPluginConstants.ATX_3
import org.jetbrains.changelog.ChangelogPluginConstants.NEW_LINE
import javax.inject.Inject

open class InitializeChangelogTask @Inject constructor(
    objectFactory: ObjectFactory,
) : DefaultTask() {

    @OutputFile
    @Optional
    val outputFile: RegularFileProperty = objectFactory.fileProperty()

    @Input
    @Optional
    val itemPrefix: Property<String> = objectFactory.property(String::class.java)

    @Input
    @Optional
    val groups: ListProperty<String> = objectFactory.listProperty(String::class.java)

    @Input
    @Optional
    val unreleasedTerm: Property<String> = objectFactory.property(String::class.java)

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
