package org.jetbrains.changelog.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.property
import org.jetbrains.changelog.Changelog
import javax.inject.Inject

open class GetChangelogTask @Inject constructor(
    objectFactory: ObjectFactory,
) : DefaultTask() {

    @Input
    @Option(option = "no-header", description = "Omits header version line")
    var noHeader = false

    @Input
    @Option(option = "unreleased", description = "Returns Unreleased change notes")
    var unreleased = false

    @InputFile
    @Optional
    val inputFile = objectFactory.fileProperty()

    @Input
    @Optional
    val headerParserRegex = objectFactory.property<Regex>()

    @Input
    @Optional
    val itemPrefix = objectFactory.property<String>()

    @Input
    @Optional
    val unreleasedTerm = objectFactory.property<String>()

    @Input
    @Optional
    val version = objectFactory.property<String>()

    @TaskAction
    fun run() = logger.quiet(
        Changelog(
            inputFile.get().asFile,
            unreleasedTerm.get(),
            headerParserRegex.get(),
            itemPrefix.get(),
        ).run {
            val version = when (unreleased) {
                true -> unreleasedTerm
                false -> version
            }.get()
            get(version).run {
                withHeader(!noHeader)
                toText()
            }
        }
    )
}
