// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.PluginInstantiationException
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.jetbrains.changelog.ChangelogPluginConstants.EXTENSION_NAME
import org.jetbrains.changelog.ChangelogPluginConstants.GET_CHANGELOG_TASK_NAME
import org.jetbrains.changelog.ChangelogPluginConstants.GROUP_NAME
import org.jetbrains.changelog.ChangelogPluginConstants.INITIALIZE_CHANGELOG_TASK_NAME
import org.jetbrains.changelog.ChangelogPluginConstants.MINIMAL_SUPPORTED_GRADLE_VERSION
import org.jetbrains.changelog.ChangelogPluginConstants.PATCH_CHANGELOG_TASK_NAME
import org.jetbrains.changelog.ChangelogPluginConstants.PLUGIN_NAME
import org.jetbrains.changelog.exceptions.VersionNotSpecifiedException
import org.jetbrains.changelog.tasks.GetChangelogTask
import org.jetbrains.changelog.tasks.InitializeChangelogTask
import org.jetbrains.changelog.tasks.PatchChangelogTask
import java.io.File

class ChangelogPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        checkGradleVersion(project)

        val extension = project.extensions.create<ChangelogPluginExtension>(EXTENSION_NAME).apply {
            groups.convention(ChangelogPluginConstants.GROUPS)
            header.convention(project.provider {
                "[${version.get()}]"
            })
            keepUnreleasedSection.convention(true)
            itemPrefix.convention(ChangelogPluginConstants.ITEM_PREFIX)
            path.convention(project.provider {
                "${project.projectDir}/${ChangelogPluginConstants.CHANGELOG_FILE_NAME}"
            })
            patchEmpty.convention(true)
            unreleasedTerm.convention(ChangelogPluginConstants.UNRELEASED_TERM)
            version.convention(
                project.provider {
                    project.version.toString().takeIf { it != Project.DEFAULT_VERSION }
                        ?: throw VersionNotSpecifiedException()
                }
            )
        }

        val pathProvider = project.layout.file(extension.path.map { File(it) })

        project.tasks.register<GetChangelogTask>(GET_CHANGELOG_TASK_NAME) {
            group = GROUP_NAME

            headerParserRegex.convention(extension.getHeaderParserRegex)
            inputFile.convention(pathProvider)
            itemPrefix.convention(extension.itemPrefix)
            unreleasedTerm.set(extension.unreleasedTerm)
            version.set(extension.version)

            outputs.upToDateWhen { false }
        }

        project.tasks.register<PatchChangelogTask>(PATCH_CHANGELOG_TASK_NAME) {
            group = GROUP_NAME

            groups.set(extension.groups)
            header.set(extension.header)
            headerParserRegex.convention(extension.getHeaderParserRegex)
            inputFile.convention(pathProvider)
            itemPrefix.convention(extension.itemPrefix)
            keepUnreleasedSection.set(extension.keepUnreleasedSection)
            outputFile.convention(pathProvider)
            patchEmpty.set(extension.patchEmpty)
            unreleasedTerm.set(extension.unreleasedTerm)
        }

        project.tasks.register<InitializeChangelogTask>(INITIALIZE_CHANGELOG_TASK_NAME) {
            group = GROUP_NAME

            groups.set(extension.groups)
            outputFile.convention(pathProvider)
            itemPrefix.set(extension.itemPrefix)
            unreleasedTerm.set(extension.unreleasedTerm)
        }
    }

    private fun checkGradleVersion(project: Project) {
        if (Version.parse(project.gradle.gradleVersion) < Version.parse("6.7.1")) {
            throw PluginInstantiationException("$PLUGIN_NAME requires Gradle $MINIMAL_SUPPORTED_GRADLE_VERSION and higher")
        }
    }
}
