// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.jetbrains.changelog.ChangelogPluginConstants.SEM_VER_REGEX
import org.jetbrains.changelog.exceptions.MissingVersionException
import java.util.regex.Pattern

abstract class ChangelogPluginExtension {

    /**
     * Version prefix used to compare tags.
     *
     * Default value: `v`
     */
    @get:Optional
    abstract val versionPrefix: Property<String>

    /**
     * Current version. By default, project's version is used.
     *
     * Default value: `project.version`
     */
    @get:Optional
    abstract val version: Property<String>

    /**
     * Path to the changelog file.
     *
     * Default value: `"${project.projectDir}/CHANGELOG.md"`
     */
    @get:Optional
    abstract val path: Property<String>

    /**
     * Optional content placed before the [title].
     */
    @get:Optional
    abstract val preTitle: Property<String>

    /**
     * The changelog title set as the top-lever header – `#`.
     *
     * Default value: `"Changelog"`
     */
    @get:Optional
    abstract val title: Property<String>

    /**
     * Optional content placed after the [title].
     */
    @get:Optional
    abstract val introduction: Property<String>

    /**
     * Header value used when patching the *Unreleased* section with text containing the current version.
     *
     * Default value: `provider { "${version.get()} - ${date()}" }`
     */
    @get:Optional
    abstract val header: Property<String>

    /**
     * `Regex`/`Pattern`/`String` used to extract version from the header string.
     *
     * Default value: `null`, fallbacks to [ChangelogPluginConstants.SEM_VER_REGEX]
     */
    @get:Optional
    abstract val headerParserRegex: Property<Any>

    /**
     * Unreleased section name.
     *
     * Default value: [ChangelogPluginConstants.UNRELEASED_TERM]
     */
    @get:Optional
    abstract val unreleasedTerm: Property<String>

    /**
     * Add an unreleased empty section on the top of the changelog after running the patching task.
     *
     * Default value: `true`
     */
    @get:Optional
    abstract val keepUnreleasedSection: Property<Boolean>

    /**
     * Patches changelog even if no release note is provided.
     *
     * Default value: `true`
     */
    @get:Optional
    abstract val patchEmpty: Property<Boolean>

    /**
     * List of groups created with a new Unreleased section.
     *
     * Default value: [ChangelogPluginConstants.GROUPS]
     */
    @get:Optional
    abstract val groups: ListProperty<String>

    /**
     * Single item's prefix, allows to customise the bullet sign.
     *
     * Default value: [ChangelogPluginConstants.ITEM_PREFIX]
     */
    @get:Optional
    abstract val itemPrefix: Property<String>

    /**
     * Combines pre-releases (like `1.0.0-alpha`, `1.0.0-beta.2`) into the final release note when patching.
     *
     * Default value: `true`
     */
    @get:Optional
    abstract val combinePreReleases: Property<Boolean>

    /**
     * Line separator used for generating changelog content.
     *
     * Default value: `"\n"` or determined from the existing file
     */
    @get:Optional
    abstract val lineSeparator: Property<String>

    /**
     * The GitHub repository URL used to build release links.
     * If provided, leads to the GitHub comparison page.
     */
    @get:Optional
    abstract val repositoryUrl: Property<String>

    /**
     * Function to build a single URL to link section with the GitHub page to present changes within the given release.
     *
     * Default value: Common [ChangelogSectionUrlBuilder] implementation
     */
    @get:Optional
    abstract val sectionUrlBuilder: Property<ChangelogSectionUrlBuilder>

    /**
     * [Changelog] instance shared between [ChangelogPluginExtension] and tasks.
     */
    @get:Internal
    abstract val instance: Property<Changelog>

    /**
     * [headerParserRegex] value normalized to [Regex] instance.
     * Falls back to [ChangelogPluginConstants.SEM_VER_REGEX]]
     */
    @get:Internal
    @Suppress("LeakingThis")
    val getHeaderParserRegex: Provider<Regex> = headerParserRegex.map {
        when (it) {
            is Regex -> it
            is String -> it.toRegex()
            is Pattern -> it.toRegex()
            else -> throw IllegalArgumentException("Unsupported type of $it. Expected value types: Regex, String, Pattern.")
        }
    }.orElse(SEM_VER_REGEX)

    fun get(version: String) = instance.get().get(version)

    fun getAll() = instance.get().items

    fun getOrNull(version: String) = instance.get().runCatching { get(version) }.getOrNull()

    fun getLatest() = instance.get().getLatest()

    fun getUnreleased() = instance.get().unreleasedItem ?: throw MissingVersionException(unreleasedTerm.get())

    fun has(version: String) = instance.get().has(version)

    fun render(outputType: Changelog.OutputType = Changelog.OutputType.MARKDOWN) = instance.get().render(outputType)

    fun renderItem(item: Changelog.Item, outputType: Changelog.OutputType = Changelog.OutputType.MARKDOWN) = instance.get().renderItem(item, outputType)
}
