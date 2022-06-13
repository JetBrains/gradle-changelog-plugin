package org.jetbrains.changelog

import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import java.io.File
import java.util.regex.Pattern
import javax.inject.Inject

open class ChangelogPluginExtension @Inject constructor(
    objectFactory: ObjectFactory,
) {

    @Optional
    val groups = objectFactory.listProperty<String>()

    @Optional
    val header = objectFactory.property<String>()

    @Optional
    val headerParserRegex = objectFactory.property<Any>()

    internal fun getHeaderParserRegex() = when (val value = headerParserRegex.orNull) {
        is Regex -> value
        is String -> value.toRegex()
        is Pattern -> value.toRegex()
        null -> ChangelogPluginConstants.SEM_VER_REGEX
        else -> throw IllegalArgumentException("Unsupported type of $value. Expected value types: Regex, String, Pattern.")
    }

    @Optional
    val itemPrefix = objectFactory.property<String>()

    @Optional
    val keepUnreleasedSection = objectFactory.property<Boolean>()

    @Optional
    val patchEmpty = objectFactory.property<Boolean>()

    @Optional
    val path = objectFactory.property<String>()

    @Optional
    val unreleasedTerm = objectFactory.property<String>()

    @Optional
    val version = objectFactory.property<String>()

    fun get(version: String) = changelog.get(version)

    fun getAll() = changelog.getAll()

    fun getOrNull(version: String) = changelog.runCatching { get(version) }.getOrNull()

    fun getLatest() = changelog.getLatest()

    fun getUnreleased() = get(unreleasedTerm.get())

    fun has(version: String) = changelog.has(version)

    private val changelog
        get() = Changelog(
            File(path.get()),
            unreleasedTerm.get(),
            getHeaderParserRegex(),
            itemPrefix.get(),
        )
}
