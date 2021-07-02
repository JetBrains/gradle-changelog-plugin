package org.jetbrains.changelog

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.changelog.exceptions.VersionNotSpecifiedException
import org.jetbrains.changelog.tasks.GetChangelogTask
import org.jetbrains.changelog.tasks.InitializeChangelogTask
import org.jetbrains.changelog.tasks.PatchChangelogTask

class ChangelogPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.run {
            extensions.create("changelog", ChangelogPluginExtension::class.java).apply {
                groups.convention(listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"))
                header.convention(provider { "[${version.get()}]" })
                itemPrefix.convention("-")
                keepUnreleasedSection.convention(true)
                patchEmpty.convention(true)
                path.convention(provider { "$projectDir/CHANGELOG.md" })
                version.convention(
                    provider {
                        project.version.toString().takeIf { it != Project.DEFAULT_VERSION }
                            ?: throw VersionNotSpecifiedException()
                    }
                )
                unreleasedTerm.convention("[Unreleased]")
            }

            tasks.apply {
                register("patchChangelog", PatchChangelogTask::class.java) {
                    it.group = "changelog"
                }
                register("initializeChangelog", InitializeChangelogTask::class.java) {
                    it.group = "changelog"
                }
                register("getChangelog", GetChangelogTask::class.java) {
                    it.group = "changelog"
                    it.outputs.upToDateWhen { false }
                }
            }
        }
    }
}
