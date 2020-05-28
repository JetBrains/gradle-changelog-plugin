package org.jetbrains.changelog

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.changelog.tasks.GetChangelogTask
import org.jetbrains.changelog.tasks.PatchChangelogTask

class ChangelogPlugin : Plugin<Project> {

    private lateinit var project: Project

    override fun apply(project: Project) {
        this.project = project

        project.run {
            extensions.create("changelog", ChangelogPluginExtension::class.java, project)
            tasks.create("patchChangelog", PatchChangelogTask::class.java) {
                it.group = "build"
            }
            tasks.create("getChangelog", GetChangelogTask::class.java) {
                it.group = "build"
                it.outputs.upToDateWhen { false }
            }
        }
    }
}
