package org.jetbrains.changelog

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Optional
import java.io.File
import java.util.regex.Pattern

@Suppress("UnstableApiUsage")
open class ChangelogPluginExtension(objects: ObjectFactory) {

    @Optional
    val groups: ListProperty<String> = objects.listProperty(String::class.java)

    @Optional
    val header: Property<String> = objects.property(String::class.java)

    @Optional
    val headerParserRegex: Property<Any> = objects.property(Any::class.java)

    internal fun getHeaderParserRegex() = when (val value = headerParserRegex.orNull) {
        is Regex -> value
        is String -> value.toRegex()
        is Pattern -> value.toRegex()
        null -> ChangelogPluginConstants.SEM_VER_REGEX
        else -> throw IllegalArgumentException("Unsupported type of $value. Expected value types: Regex, String, Pattern.")
    }

    @Optional
    val itemPrefix: Property<String> = objects.property(String::class.java)

    @Optional
    val keepUnreleasedSection: Property<Boolean> = objects.property(Boolean::class.java)

    @Optional
    val patchEmpty: Property<Boolean> = objects.property(Boolean::class.java)

    @Optional
    val path: Property<String> = objects.property(String::class.java)

    @Optional
    val version: Property<String> = objects.property(String::class.java)

    @Optional
    val unreleasedTerm: Property<String> = objects.property(String::class.java)

    fun getUnreleased() = get(unreleasedTerm.get())

    fun get(version: String) = changelog.get(version)

    fun getOrNull(version: String) = changelog.run {
        if (has(version)) {
            get(version)
        } else {
            null
        }
    }

    fun getLatest() = changelog.getLatest()

    fun getAll() = changelog.getAll()

    fun has(version: String) = changelog.has(version)

    private val changelog
        get() = Changelog(
            File(path.get()),
            unreleasedTerm.get(),
            getHeaderParserRegex(),
            itemPrefix.get(),
        )
}
