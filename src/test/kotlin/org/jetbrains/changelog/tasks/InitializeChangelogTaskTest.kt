// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.tasks

import org.gradle.kotlin.dsl.assign
import org.jetbrains.changelog.BaseTest
import org.jetbrains.changelog.ChangelogPluginConstants.INITIALIZE_CHANGELOG_TASK_NAME
import org.jetbrains.changelog.normalizeLineSeparator
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
            """.trimIndent()

        project.evaluate()
    }

    @Test
    fun `creates new changelog file`() {
        runTask(INITIALIZE_CHANGELOG_TASK_NAME)

        assertMarkdown(
            """
            # Changelog
            
            ## Unreleased
            
            ### Added
            
            ### Changed
            
            ### Deprecated
            
            ### Removed
            
            ### Fixed
            
            ### Security

            """.trimIndent(),
            extension.render(),
        )

        assertMarkdown(
            """
            ## Unreleased
            
            ### Added
            
            ### Changed
            
            ### Deprecated
            
            ### Removed
            
            ### Fixed
            
            ### Security
            
            """.trimIndent(),
            extension.renderItem(extension.getUnreleased()),
        )

        extension.getAll().apply {
            assertEquals(1, keys.size)
            assertEquals("Unreleased", keys.first())
            assertMarkdown(
                """
                ## Unreleased
                
                ### Added
                
                ### Changed
                
                ### Deprecated
                
                ### Removed
                
                ### Fixed
                
                ### Security

                """.trimIndent(),
                extension.renderItem(values.first()),
            )
        }

        assertNotNull(extension.getUnreleased())
    }

    @Test
    fun `overrides existing changelog file`() {
        changelog =
            """
            # Changelog
            """.trimIndent()
        project.evaluate()

        val result = runFailingTask(INITIALIZE_CHANGELOG_TASK_NAME)

        assertTrue(result.output.contains("org.gradle.api.GradleException: Changelog file is not empty: ${changelogFile.absolutePath}"))
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
                path = file("CHANGES.md").canonicalPath
                itemPrefix = "*"
                unreleasedTerm = "Upcoming version"
                groups = ["Added", "Removed"]
                title = "My Title"
                preTitle = "Foo"
                introduction = "Introduction"
            }
            """.trimIndent()
        extension.apply {
            path = File("${project.projectDir}/CHANGES.md").path
            unreleasedTerm = "Upcoming version"
            itemPrefix = "*"
        }
        project.evaluate()

        runTask(INITIALIZE_CHANGELOG_TASK_NAME)

        assertMarkdown(
            """
            Foo
            
            # My Title
            
            Introduction
            
            ## Upcoming version
            
            ### Added
            
            ### Removed
            
            """.trimIndent(),
            extension.render(),
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
            """.trimIndent()

        project.evaluate()

        val result = runTask(INITIALIZE_CHANGELOG_TASK_NAME)

        assertFalse(result.output.contains("VersionNotSpecifiedException"))
    }

    @Test
    fun `task loads from the configuration cache`() {
        runTask(INITIALIZE_CHANGELOG_TASK_NAME)
        runTask(INITIALIZE_CHANGELOG_TASK_NAME)
        val result = runTask(INITIALIZE_CHANGELOG_TASK_NAME)

        assertTrue(result.output.contains("Reusing configuration cache."))
    }

    @Test
    fun `creates new changelog file with CRLF line separator`() {
        extension.lineSeparator = "\r\n"

        runTask(INITIALIZE_CHANGELOG_TASK_NAME)

        assertMarkdown(
            """
            # Changelog
            
            ## Unreleased
            
            ### Added
            
            ### Changed
            
            ### Deprecated
            
            ### Removed
            
            ### Fixed
            
            ### Security

            """.trimIndent().normalizeLineSeparator("\r\n"),
            extension.render(),
        )

        assertMarkdown(
            """
            ## Unreleased
            
            ### Added
            
            ### Changed
            
            ### Deprecated
            
            ### Removed
            
            ### Fixed
            
            ### Security
            
            """.trimIndent().normalizeLineSeparator("\r\n"),
            extension.renderItem(extension.getUnreleased()),
        )
    }

    @Test
    fun `creates new changelog file with CR line separator`() {
        extension.lineSeparator = "\r"

        runTask(INITIALIZE_CHANGELOG_TASK_NAME)

        assertMarkdown(
            """
            # Changelog
            
            ## Unreleased
            
            ### Added
            
            ### Changed
            
            ### Deprecated
            
            ### Removed
            
            ### Fixed
            
            ### Security

            """.trimIndent().normalizeLineSeparator("\r"),
            extension.render(),
        )

        assertMarkdown(
            """
            ## Unreleased
            
            ### Added
            
            ### Changed
            
            ### Deprecated
            
            ### Removed
            
            ### Fixed
            
            ### Security
            
            """.trimIndent().normalizeLineSeparator("\r"),
            extension.renderItem(extension.getUnreleased()),
        )
    }
}
