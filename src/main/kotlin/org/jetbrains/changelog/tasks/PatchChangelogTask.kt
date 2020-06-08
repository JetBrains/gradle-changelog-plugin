package org.jetbrains.changelog.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.ChangelogPluginExtension
import java.io.File

open class PatchChangelogTask : DefaultTask() {

    private val extension = project.extensions.getByType(ChangelogPluginExtension::class.java)

    init {
        group = "build"
    }

    @InputFile
    fun getInputFile() = File(extension.path)

    @OutputFile
    fun getOutputFile() = getInputFile()

    @TaskAction
    fun run() {
        Changelog(extension).apply {
            get(extension.unreleasedTerm).let {
                val header = it.getHeaderNode()
                val versionHeader = extension.headerFormat().format(arrayOf(extension.version))

                File(extension.path).writeText(content.run {
                    if (extension.keepUnreleasedSection) {
                        StringBuilder(this).insert(header.endOffset, "\n\n$versionHeader").toString()
                    } else {
                        replaceRange(header.startOffset, header.endOffset, versionHeader)
                    }
                })
            }
        }
    }
}
