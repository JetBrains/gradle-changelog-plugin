// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.jetbrains.changelog.ChangelogPluginConstants.SEM_VER_REGEX
import org.jetbrains.changelog.exceptions.MissingVersionException
import java.util.regex.Pattern

abstract class ChangelogPluginExtension {

    @get:Optional
    abstract val groups: ListProperty<String>

    @get:Optional
    abstract val preTitle: Property<String>

    @get:Optional
    abstract val title: Property<String>

    @get:Optional
    abstract val header: Property<String>

    @get:Optional
    abstract val headerParserRegex: Property<Any>

    @get:Optional
    abstract val introduction: Property<String>

    @get:Internal
    @Suppress("LeakingThis")
    val getHeaderParserRegex = headerParserRegex.map {
        when (it) {
            is Regex -> it
            is String -> it.toRegex()
            is Pattern -> it.toRegex()
            else -> throw IllegalArgumentException("Unsupported type of $it. Expected value types: Regex, String, Pattern.")
        }
    }.orElse(SEM_VER_REGEX)

    @get:Optional
    abstract val itemPrefix: Property<String>

    @get:Optional
    abstract val keepUnreleasedSection: Property<Boolean>

    @get:Optional
    abstract val patchEmpty: Property<Boolean>

    @get:Optional
    abstract val path: Property<String>

    @get:Optional
    abstract val unreleasedTerm: Property<String>

    @get:Optional
    abstract val version: Property<String>

    @get:Optional
    abstract val lineSeparator: Property<String>

    @get:Optional
    abstract val combinePreReleases: Property<Boolean>

    @get:Optional
    abstract val repositoryUrl: Property<String>

    @get:Optional
    abstract val sectionUrlBuilder: Property<ChangelogSectionUrlBuilder>

    @get:Internal
    abstract val instance: Property<Changelog>

    fun get(version: String) = instance.get().get(version)

    fun getAll() = instance.get().items

    fun getOrNull(version: String) = instance.get().runCatching { get(version) }.getOrNull()

    fun getLatest() = instance.get().getLatest()

    fun getUnreleased() = instance.get().unreleasedItem ?: throw MissingVersionException(unreleasedTerm.get())

    fun has(version: String) = instance.get().has(version)

    fun render(outputType: Changelog.OutputType = Changelog.OutputType.MARKDOWN) = instance.get().render(outputType)

    fun renderItem(item: Changelog.Item, outputType: Changelog.OutputType = Changelog.OutputType.MARKDOWN) = instance.get().renderItem(item, outputType)
}
