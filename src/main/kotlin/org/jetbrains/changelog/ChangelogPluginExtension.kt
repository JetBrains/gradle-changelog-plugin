package org.jetbrains.changelog

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

@Suppress("UnstableApiUsage")
open class ChangelogPluginExtension(private val project: Project) {

    @Optional
    @Internal
    private val groupsProperty: ListProperty<String> = project.objects.listProperty(String::class.java)
    var groups: List<String>
        get() = groupsProperty.getOrElse(emptyList()).ifEmpty {
            listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security")
        }
        set(value) = groupsProperty.set(value)

    @Optional
    @Internal
    private val headerProperty: Property<Closure<*>> = project.objects.property(Closure::class.java).apply {
        set(closure { "[$version]" })
    }
    var header: Closure<*>
        get() = headerProperty.get()
        set(value) = headerProperty.set(value)

    @Optional
    @Internal
    private val headerParserRegexProperty: Property<Regex?> = project.objects.property(Regex::class.java)
    var headerParserRegex: Regex?
        get() = headerParserRegexProperty.orNull
        set(value) = headerParserRegexProperty.set(value)

    @Optional
    @Internal
    private val itemPrefixProperty: Property<String> = project.objects.property(String::class.java).apply {
        set("-")
    }
    var itemPrefix: String
        get() = itemPrefixProperty.get()
        set(value) = itemPrefixProperty.set(value)

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
    private val patchEmptyProperty: Property<Boolean> = project.objects.property(Boolean::class.java).apply {
        set(true)
    }
    var patchEmpty: Boolean
        get() = patchEmptyProperty.get()
        set(value) = patchEmptyProperty.set(value)

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
        set("[Unreleased]")
    }
    var unreleasedTerm: String
        get() = unreleasedTermProperty.get()
        set(value) = unreleasedTermProperty.set(value)

    fun getUnreleased() = get(unreleasedTerm)

    fun get(version: String = this.version) = Changelog(this).get(version)

    fun getLatest() = Changelog(this).getLatest()

    fun getAll() = Changelog(this).getAll()

    fun has(version: String) = Changelog(this).has(version)
}
