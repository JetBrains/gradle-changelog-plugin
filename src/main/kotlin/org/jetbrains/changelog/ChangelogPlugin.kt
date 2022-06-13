package org.jetbrains.changelog

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.PluginInstantiationException
import org.jetbrains.changelog.exceptions.VersionNotSpecifiedException
import org.jetbrains.changelog.tasks.GetChangelogTask
import org.jetbrains.changelog.tasks.InitializeChangelogTask
import org.jetbrains.changelog.tasks.PatchChangelogTask

class ChangelogPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        checkGradleVersion(project)

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
            group = ChangelogPluginConstants.GROUP_NAME
            outputs.upToDateWhen { false }

            headerParserRegex.convention(project.provider {
                extension.getHeaderParserRegex()
            })
            inputFile.convention {
                project.file(extension.path.get())
            }
            itemPrefix.convention(extension.itemPrefix)
            unreleasedTerm.set(extension.unreleasedTerm)
            version.set(extension.version)
        }

        project.tasks.register(ChangelogPluginConstants.PATCH_CHANGELOG_TASK_NAME, PatchChangelogTask::class.java) {
            group = ChangelogPluginConstants.GROUP_NAME

            groups.set(extension.groups)
            header.set(extension.header)
            headerParserRegex.convention(project.provider {
                extension.getHeaderParserRegex()
            })
            inputFile.convention {
                project.file(extension.path.get())
            }
            itemPrefix.convention(extension.itemPrefix)
            keepUnreleasedSection.set(extension.keepUnreleasedSection)
            outputFile.convention {
                project.file(extension.path.get())
            }
            patchEmpty.set(extension.patchEmpty)
            unreleasedTerm.set(extension.unreleasedTerm)
        }

        project.tasks.register(ChangelogPluginConstants.INITIALIZE_CHANGELOG_TASK_NAME, InitializeChangelogTask::class.java) {
            group = ChangelogPluginConstants.GROUP_NAME

            groups.set(extension.groups)
            outputFile.convention {
                project.file(extension.path.get())
            }
            itemPrefix.set(extension.itemPrefix)
            unreleasedTerm.set(extension.unreleasedTerm)
        }
    }

    private fun checkGradleVersion(project: Project) {
        if (Version.parse(project.gradle.gradleVersion) < Version.parse("6.7.1")) {
            throw PluginInstantiationException("Gradle Changelog Plugin requires Gradle 6.7.1 and higher")
        }
    }
}
