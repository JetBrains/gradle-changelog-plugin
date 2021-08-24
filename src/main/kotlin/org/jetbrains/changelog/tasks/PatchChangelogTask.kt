package org.jetbrains.changelog.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.StopActionException
import org.gradle.api.tasks.TaskAction
import org.jetbrains.changelog.Changelog
import javax.inject.Inject

open class PatchChangelogTask @Inject constructor(
    objectFactory: ObjectFactory,
) : DefaultTask() {

    @InputFile
    @Optional
    val inputFile: RegularFileProperty = objectFactory.fileProperty()

    @OutputFile
    @Optional
    val outputFile: RegularFileProperty = objectFactory.fileProperty()

    @Input
    @Optional
    val groups: ListProperty<String> = objectFactory.listProperty(String::class.java)

    @Input
    @Optional
    val header: Property<String> = objectFactory.property(String::class.java)

    @Input
    @Optional
    val headerParserRegex: Property<Regex> = objectFactory.property(Regex::class.java)

    @Input
    @Optional
    val itemPrefix: Property<String> = objectFactory.property(String::class.java)

    @Input
    @Optional
    val keepUnreleasedSection: Property<Boolean> = objectFactory.property(Boolean::class.java)

    @Input
    @Optional
    val patchEmpty: Property<Boolean> = objectFactory.property(Boolean::class.java)

    @Input
    @Optional
    val unreleasedTerm: Property<String> = objectFactory.property(String::class.java)

    @TaskAction
    fun run() {
        Changelog(
            inputFile.get().asFile,
            unreleasedTerm.get(),
            headerParserRegex.get(),
            itemPrefix.get(),
        ).apply {
            val unreleasedTerm = unreleasedTerm.get()
            if (!has(unreleasedTerm)) {
                logger.warn(
                    ":patchChangelog task requires '$unreleasedTerm' section to be present. " +
                        "Add '## $unreleasedTerm' section header to your changelog file: ${inputFile.get().asFile.canonicalPath}"
                )
                throw StopActionException()
            }
            get(unreleasedTerm).let { item ->
                val node = item.getHeaderNode()
                val header = "## ${header.get()}"

                if (item.getSections().isEmpty() && !patchEmpty.get()) {
                    return
                }

                outputFile.get().asFile.writeText(
                    this.content.run {
                        if (keepUnreleasedSection.get()) {
                            val unreleasedGroups = groups.get().joinToString("\n") { "### $it\n" }
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
