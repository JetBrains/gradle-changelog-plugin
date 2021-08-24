package org.jetbrains.changelog

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.changelog.exceptions.VersionNotSpecifiedException
import org.jetbrains.changelog.tasks.GetChangelogTask
import org.jetbrains.changelog.tasks.InitializeChangelogTask
import org.jetbrains.changelog.tasks.PatchChangelogTask

class ChangelogPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create(ChangelogPluginConstants.EXTENSION_NAME, ChangelogPluginExtension::class.java).apply {
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

        project.tasks.register(ChangelogPluginConstants.GET_CHANGELOG_TASK_NAME, GetChangelogTask::class.java) {
            it.group = ChangelogPluginConstants.GROUP_NAME
            it.outputs.upToDateWhen { false }

            it.headerParserRegex.convention(project.provider {
                extension.getHeaderParserRegex()
            })
            it.inputFile.convention {
                project.file(extension.path.get())
            }
            it.itemPrefix.convention(extension.itemPrefix)
            it.unreleasedTerm.set(extension.unreleasedTerm)
            it.version.set(extension.version)
        }

        project.tasks.register(ChangelogPluginConstants.PATCH_CHANGELOG_TASK_NAME, PatchChangelogTask::class.java) {
            it.group = ChangelogPluginConstants.GROUP_NAME

            it.groups.set(extension.groups)
            it.header.set(extension.header)
            it.headerParserRegex.convention(project.provider {
                extension.getHeaderParserRegex()
            })
            it.inputFile.convention {
                project.file(extension.path.get())
            }
            it.itemPrefix.convention(extension.itemPrefix)
            it.keepUnreleasedSection.set(extension.keepUnreleasedSection)
            it.outputFile.convention {
                project.file(extension.path.get())
            }
            it.patchEmpty.set(extension.patchEmpty)
            it.unreleasedTerm.set(extension.unreleasedTerm)
        }

        project.tasks.register(ChangelogPluginConstants.INITIALIZE_CHANGELOG_TASK_NAME, InitializeChangelogTask::class.java) {
            it.group = ChangelogPluginConstants.GROUP_NAME

            it.groups.set(extension.groups)
            it.outputFile.convention {
                project.file(extension.path.get())
            }
            it.itemPrefix.set(extension.itemPrefix)
            it.unreleasedTerm.set(extension.unreleasedTerm)
        }
    }
}
