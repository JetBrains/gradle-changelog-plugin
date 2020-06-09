package org.jetbrains.changelog

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import java.text.MessageFormat

@Suppress("UnstableApiUsage")
open class ChangelogPluginExtension(private val project: Project) {

    @Optional
    @Internal
    private val formatProperty: Property<String> = project.objects.property(String::class.java).apply {
        set("[{0}]")
    }
    var format: String
        get() = formatProperty.get()
        set(value) = formatProperty.set(value)

    @Internal
    fun headerFormat() = MessageFormat("## $format")

    @Optional
    @Internal
    private val keepUnreleasedSectionProperty: Property<Boolean> = project.objects.property(Boolean::class.java).apply {
        set(true)
    }
    var keepUnreleasedSection: Boolean
        get() = keepUnreleasedSectionProperty.get()
        set(value) = keepUnreleasedSectionProperty.set(value)

    @Optional
    @Internal
    private val pathProperty: Property<String> = project.objects.property(String::class.java).apply {
        set("${project.projectDir}/CHANGELOG.md")
    }
    var path: String
        get() = pathProperty.get()
        set(value) = pathProperty.set(value)

    @Optional
    @Internal
    private val versionProperty: Property<String> = project.objects.property(String::class.java)
    var version: String
        get() = versionProperty.getOrElse(project.version.toString())
        set(value) = versionProperty.set(value)

    @Optional
    @Internal
    private val unreleasedTermProperty: Property<String> = project.objects.property(String::class.java).apply {
        set("Unreleased")
    }
    var unreleasedTerm: String
        get() = unreleasedTermProperty.get()
        set(value) = unreleasedTermProperty.set(value)

    fun getUnreleased() = get(unreleasedTerm)

    fun get(version: String = this.version) = Changelog(this).get(version)

    fun getLatest() = Changelog(this).getLatest()
}
