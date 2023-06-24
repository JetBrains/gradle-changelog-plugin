// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.PluginInstantiationException
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.util.GradleVersion
import org.jetbrains.changelog.ChangelogPluginConstants.CHANGELOG_FILE_NAME
import org.jetbrains.changelog.ChangelogPluginConstants.EXTENSION_NAME
import org.jetbrains.changelog.ChangelogPluginConstants.GET_CHANGELOG_TASK_NAME
import org.jetbrains.changelog.ChangelogPluginConstants.GROUPS
import org.jetbrains.changelog.ChangelogPluginConstants.GROUP_NAME
import org.jetbrains.changelog.ChangelogPluginConstants.INITIALIZE_CHANGELOG_TASK_NAME
import org.jetbrains.changelog.ChangelogPluginConstants.ITEM_PREFIX
import org.jetbrains.changelog.ChangelogPluginConstants.MINIMAL_SUPPORTED_GRADLE_VERSION
import org.jetbrains.changelog.ChangelogPluginConstants.PATCH_CHANGELOG_TASK_NAME
import org.jetbrains.changelog.ChangelogPluginConstants.PLUGIN_NAME
import org.jetbrains.changelog.ChangelogPluginConstants.UNRELEASED_TERM
import org.jetbrains.changelog.exceptions.VersionNotSpecifiedException
import org.jetbrains.changelog.tasks.GetChangelogTask
import org.jetbrains.changelog.tasks.InitializeChangelogTask
import org.jetbrains.changelog.tasks.PatchChangelogTask
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class ChangelogPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        checkGradleVersion()

        val extension = project.extensions.create<ChangelogPluginExtension>(EXTENSION_NAME).apply {
            path.convention(project.provider {
                project.file(CHANGELOG_FILE_NAME).canonicalPath
            }.map {
                with(Path.of(it)) {
                    if (!Files.exists(this)) {
                        Files.createFile(this)
                    }
                    toAbsolutePath().toString()
                }
            })
            versionPrefix.convention(project.provider { "v" })

            version.convention(
                project.provider {
                    project.version.toString().takeIf { it != Project.DEFAULT_VERSION }
                        ?: throw VersionNotSpecifiedException()
                }
            )
            header.convention(project.provider {
                "${version.get()} - ${date()}"
            })
            unreleasedTerm.convention(UNRELEASED_TERM)
            keepUnreleasedSection.convention(true)
            patchEmpty.convention(true)
            groups.convention(GROUPS)
            itemPrefix.convention(ITEM_PREFIX)
            combinePreReleases.convention(true)
            lineSeparator.convention(path.map { path ->
                val content = Path.of(path)
                    .takeIf { Files.exists(it) }
                    ?.let { Files.readString(it) }
                    ?: return@map "\n"
                val rnless = content.replace("\r\n", "")

                val rn = (content.length - rnless.length) / 2
                val r = rnless.count { it == '\r' }
                val n = rnless.count { it == '\n' }

                when {
                    rn > r && rn > n -> "\r\n"
                    r > n -> "\r"
                    else -> "\n"
                }
            })
            repositoryUrl.map { it.removeSuffix("/") }
            sectionUrlBuilder.convention(
                ChangelogSectionUrlBuilder { repositoryUrl, currentVersion, previousVersion, isUnreleased ->
                    val prefix = versionPrefix.get()
                    repositoryUrl + when {
                        isUnreleased -> when (previousVersion) {
                            null -> "/commits"
                            else -> "/compare/$prefix$previousVersion...HEAD"
                        }

                        previousVersion == null -> "/commits/$prefix$currentVersion"

                        else -> "/compare/$prefix$previousVersion...$prefix$currentVersion"
                    }
                }
            )

            instance.convention(project.provider {
                Changelog(
                    content = path.map {
                        with(File(it)) {
                            if (!exists()) {
                                createNewFile()
                            }
                            readText()
                        }
                    }.get(),
                    defaultPreTitle = preTitle.orNull,
                    defaultTitle = title.orNull,
                    defaultIntroduction = introduction.orNull,
                    unreleasedTerm = unreleasedTerm.get(),
                    groups = groups.get(),
                    headerParserRegex = getHeaderParserRegex.get(),
                    itemPrefix = itemPrefix.get(),
                    repositoryUrl = repositoryUrl.orNull,
                    sectionUrlBuilder = sectionUrlBuilder.get(),
                    lineSeparator = lineSeparator.get(),
                )
            })
        }

        val pathProvider = project.layout.file(extension.path.map { File(it) })

        project.tasks.register<GetChangelogTask>(GET_CHANGELOG_TASK_NAME) {
            group = GROUP_NAME

            unreleasedTerm.convention(extension.unreleasedTerm)
            changelog.convention(extension.instance)
            inputFile.convention(pathProvider)

            outputs.upToDateWhen { false }
        }

        project.tasks.register<PatchChangelogTask>(PATCH_CHANGELOG_TASK_NAME) {
            group = GROUP_NAME

            version.convention(extension.version)
            header.convention(extension.header)
            keepUnreleasedSection.convention(extension.keepUnreleasedSection)
            patchEmpty.convention(extension.patchEmpty)
            combinePreReleases.convention(extension.combinePreReleases)
            unreleasedTerm.convention(extension.unreleasedTerm)
            changelog.convention(extension.instance)
            inputFile.convention(pathProvider)
            outputFile.convention(pathProvider)
        }

        project.tasks.register<InitializeChangelogTask>(INITIALIZE_CHANGELOG_TASK_NAME) {
            group = GROUP_NAME

            unreleasedTerm.convention(extension.unreleasedTerm)
            changelog.convention(extension.instance)
            outputFile.convention(pathProvider)
        }
    }

    private fun checkGradleVersion() {
        if (GradleVersion.current() < GradleVersion.version(MINIMAL_SUPPORTED_GRADLE_VERSION)) {
            throw PluginInstantiationException("$PLUGIN_NAME requires Gradle $MINIMAL_SUPPORTED_GRADLE_VERSION and higher")
        }
    }
}
