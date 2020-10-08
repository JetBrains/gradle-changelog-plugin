package org.jetbrains.changelog.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.StopActionException
import org.gradle.api.tasks.TaskAction
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.ChangelogPluginExtension
import java.io.File

open class PatchChangelogTask : DefaultTask() {

    private val extension = project.extensions.getByType(ChangelogPluginExtension::class.java)

    @InputFile
    fun getInputFile() = File(extension.path)

    @OutputFile
    fun getOutputFile() = getInputFile()

    @TaskAction
    fun run() {
        Changelog(extension).apply {
            if (!has(extension.unreleasedTerm)) {
                logger.warn(
                    ":patchChangelog task requires '${extension.unreleasedTerm}' section to be present. " +
                        "Add '## ${extension.unreleasedTerm}' section header to your changelog file: ${extension.path}"
                )
                throw StopActionException()
            }
            get(extension.unreleasedTerm).let { item ->
                val header = item.getHeaderNode()
                val versionHeader = "## ${extension.header.call()}"

                if (extension.getUnreleased().getSections().isEmpty() && !extension.patchEmpty) {
                    return
                }

                File(extension.path).writeText(
                    content.run {
                        if (extension.keepUnreleasedSection) {
                            val unreleasedGroups = extension.groups.joinToString("\n") { "### $it\n" }
                            StringBuilder(this).insert(header.endOffset, "\n$unreleasedGroups$versionHeader").toString()
                        } else {
                            replaceRange(header.startOffset, header.endOffset, versionHeader)
                        }
                    }
                )
            }
        }
    }
}
