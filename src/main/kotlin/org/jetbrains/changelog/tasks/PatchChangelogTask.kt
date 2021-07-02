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
    fun getInputFile() = File(extension.path.get())

    @OutputFile
    fun getOutputFile() = getInputFile()

    @TaskAction
    fun run() {
        Changelog(extension).apply {
            val unreleasedTerm = extension.unreleasedTerm.get()
            if (!has(unreleasedTerm)) {
                logger.warn(
                    ":patchChangelog task requires '$unreleasedTerm' section to be present. " +
                        "Add '## $unreleasedTerm' section header to your changelog file: ${extension.path.get()}"
                )
                throw StopActionException()
            }
            get(unreleasedTerm).let { item ->
                val node = item.getHeaderNode()
                val header = "## ${extension.header.get()}"

                if (extension.getUnreleased().getSections().isEmpty() && !extension.patchEmpty.get()) {
                    return
                }

                File(extension.path.get()).writeText(
                    this.content.run {
                        if (extension.keepUnreleasedSection.get()) {
                            val unreleasedGroups = extension.groups.get().joinToString("\n") { "### $it\n" }
                            StringBuilder(this).insert(node.endOffset, "\n$unreleasedGroups$header").toString()
                        } else {
                            replaceRange(node.startOffset, node.endOffset, header)
                        }
                    }
                )
            }
        }
    }
}
