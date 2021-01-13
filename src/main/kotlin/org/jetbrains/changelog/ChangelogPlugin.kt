package org.jetbrains.changelog

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.internal.IConventionAware
import org.jetbrains.changelog.tasks.GetChangelogTask
import org.jetbrains.changelog.tasks.InitializeChangelogTask
import org.jetbrains.changelog.tasks.PatchChangelogTask

class ChangelogPlugin : Plugin<Project> {

    private lateinit var project: Project

    override fun apply(project: Project) {
        this.project = project

        project.run {
            val extension = extensions.create("changelog", ChangelogPluginExtension::class.java, objects, projectDir, version)
            conventionMappingOf(extension).map("version") { version }

            tasks.apply {
                create("patchChangelog", PatchChangelogTask::class.java) {
                    it.group = "changelog"
                }
                create("initializeChangelog", InitializeChangelogTask::class.java) {
                    it.group = "changelog"
                }
                create("getChangelog", GetChangelogTask::class.java) {
                    it.group = "changelog"
                    it.outputs.upToDateWhen { false }
                }
            }
        }
    }

    companion object {
        fun conventionMappingOf(obj: Any): ConventionMapping = (obj as IConventionAware).conventionMapping
    }
}
