// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.tasks

import org.jetbrains.changelog.BaseTest
import org.jetbrains.changelog.ChangelogPluginConstants.INITIALIZE_CHANGELOG_TASK_NAME
import java.io.File
import kotlin.test.*

class InitializeChangelogTaskTest : BaseTest() {

    @BeforeTest
    fun localSetUp() {
        buildFile =
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
            }
            """

        project.evaluate()
    }

    @Test
    fun `creates new changelog file`() {
        runTask(INITIALIZE_CHANGELOG_TASK_NAME)

        extension.getAll().apply {
            assertEquals(1, keys.size)
            assertEquals("[Unreleased]", keys.first())
            assertEquals(
                """
                ## [Unreleased]
                
                ### Added
                - Example item

                ### Changed

                ### Deprecated

                ### Removed

                ### Fixed

                ### Security
                """.trimIndent(),
                values.first().withHeader(true).toText()
            )
        }

        assertNotNull(extension.getUnreleased())
    }

    @Test
    fun `overrides existing changelog file`() {
        changelog =
            """
            # Changelog
            """
        project.evaluate()

        runTask(INITIALIZE_CHANGELOG_TASK_NAME)

        assertEquals(
            """
            ## [Unreleased]

            ### Added
            - Example item

            ### Changed

            ### Deprecated

            ### Removed

            ### Fixed

            ### Security
            """.trimIndent(),
            extension.getUnreleased().withHeader(true).toText()
        )
    }

    @Test
    fun `creates customized changelog file`() {
        buildFile =
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                path = "${File("${project.projectDir}/CHANGES.md").path.replace("\\", "\\\\")}"
                itemPrefix = "*"
                unreleasedTerm = "Upcoming version"
                groups = ["Added", "Removed"]
            }
            """
        extension.apply {
            path.set(File("${project.projectDir}/CHANGES.md").path)
            unreleasedTerm.set("Upcoming version")
            itemPrefix.set("*")
        }
        project.evaluate()

        runTask(INITIALIZE_CHANGELOG_TASK_NAME)

        assertEquals(
            """
            ## Upcoming version

            ### Added
            * Example item
            
            ### Removed
            """.trimIndent(),
            extension.getUnreleased().withHeader(true).toText()
        )
    }

    @Test
    fun `doesn't throw VersionNotSpecifiedException when changelog extension has no version provided`() {
        buildFile =
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
            }
            """

        project.evaluate()

        val result = runTask(INITIALIZE_CHANGELOG_TASK_NAME)

        assertFalse(result.output.contains("VersionNotSpecifiedException"))
    }

    @Test
    fun `task loads from the configuration cache`() {
        runTask(INITIALIZE_CHANGELOG_TASK_NAME)
        val result = runTask(INITIALIZE_CHANGELOG_TASK_NAME)

        assertTrue(result.output.contains("Reusing configuration cache."))
    }
}
