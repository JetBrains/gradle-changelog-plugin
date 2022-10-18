// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.tasks

import org.jetbrains.changelog.BaseTest
import org.jetbrains.changelog.ChangelogPluginConstants.INITIALIZE_CHANGELOG_TASK_NAME
import java.io.File
import kotlin.test.*

class InitializeChangelogTaskTest : BaseTest() {

    @BeforeTest
    fun localSetUp() {
        buildFile = //language=groovy
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
            //language=markdown
            assertEquals(
                """
                ## [Unreleased]
                
                ### Added

                ### Changed

                ### Deprecated

                ### Removed

                ### Fixed

                ### Security
                
                """.trimIndent(),
                values.first().toText()
            )
        }

        assertNotNull(extension.getUnreleased())
    }

    @Test
    fun `overrides existing changelog file`() {
        changelog = //language=markdown
            """
            # Changelog
            """.trimIndent()
        project.evaluate()

        runTask(INITIALIZE_CHANGELOG_TASK_NAME)

        //language=markdown
        assertEquals(
            """
            ## [Unreleased]

            ### Added

            ### Changed

            ### Deprecated

            ### Removed

            ### Fixed

            ### Security
            
            """.trimIndent(),
            extension.getUnreleased().toText()
        )
    }

    @Test
    fun `creates customized changelog file`() {
        buildFile = //language=Groovy
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
                title = "My Title"
                preTitle = "Foo"
                introduction = "Introduction"
            }
            """.trimIndent()
        extension.apply {
            path.set(File("${project.projectDir}/CHANGES.md").path)
            unreleasedTerm.set("Upcoming version")
            itemPrefix.set("*")
        }
        project.evaluate()

        runTask(INITIALIZE_CHANGELOG_TASK_NAME)

        //language=markdown
        assertEquals(
            """
            Foo

            # My Title
            Introduction
            
            ## Upcoming version
            
            ### Added
            
            ### Removed
            
            """.trimIndent(),
            extension.instance.content
        )
    }

    @Test
    fun `doesn't throw VersionNotSpecifiedException when changelog extension has no version provided`() {
        buildFile = //language=Groovy
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
            }
            """.trimIndent()

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
