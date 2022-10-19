// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import java.io.File
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
    }.orElse(ChangelogPluginConstants.SEM_VER_REGEX)

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

    val instance
        get() = Changelog(
            file = File(path.get()),
            preTitle = preTitle.orNull,
            title = title.orNull,
            introduction = introduction.orNull,
            unreleasedTerm = unreleasedTerm.get(),
            headerParserRegex = getHeaderParserRegex.get(),
            itemPrefix = itemPrefix.get(),
            lineSeparator = lineSeparator.get(),
        )

    fun get(version: String) = instance.get(version)

    fun getAll() = instance.getAll()

    fun getOrNull(version: String) = instance.runCatching { get(version) }.getOrNull()

    fun getLatest() = instance.getLatest()

    fun getUnreleased() = get(unreleasedTerm.get())

    fun has(version: String) = instance.has(version)
}
