package org.jetbrains.changelog

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Optional
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
        null -> null
        else -> throw IllegalArgumentException(
            "Unsupported type of $value. Expected value types: Regex, String, Pattern."
        )
    }

    @Optional
    val itemPrefix: Property<String> = objects.property(String::class.java)

    @Optional
    val keepUnreleasedSection: Property<Boolean> = objects.property(Boolean::class.java)

    @Optional
    val patchEmpty: Property<Boolean> = objects.property(Boolean::class.java)

    @Optional
    val path: Property<String> = objects.property(String::class.java)

    val version: Property<String> = objects.property(String::class.java)

    @Optional
    val unreleasedTerm: Property<String> = objects.property(String::class.java)

    fun getUnreleased() = get(unreleasedTerm.get())

    fun get(version: String) = Changelog(this).get(version)

    fun getLatest() = Changelog(this).getLatest()

    fun getAll() = Changelog(this).getAll()

    fun has(version: String) = Changelog(this).has(version)
}
