// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.tasks

import org.jetbrains.changelog.BaseTest
import org.jetbrains.changelog.ChangelogPluginConstants.GET_CHANGELOG_TASK_NAME
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class GetChangelogTaskTest : BaseTest() {

    @BeforeTest
    fun localSetUp() {
        changelog =
            """
            # Changelog
            ## [Unreleased]
            Some unreleased changes.
            
            - bar
            
            ### Added
            
            ### Fixed
            - I fixed myself a beverage.
            
            ## [1.0.1] - 2022-10-17
            Release with bugfix.
            
            ### Fixed
            - bar
            
            ## [1.0.0] - 2022-10-10
            That was a great release.
            
            ### Added
            - foo
            
            [Unreleased]: https://jetbrians.com/Unreleased
            [1.0.1]: https://jetbrians.com/1.0.1
            [1.0.0]: https://jetbrians.com/1.0.0
            """.trimIndent()

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
    fun `returns change notes for the latest released version`() {
        val result = runTask(GET_CHANGELOG_TASK_NAME, "--quiet")

        assertMarkdown(
            """
            ## [1.0.1] - 2022-10-17
            Release with bugfix.
            
            ### Fixed
            - bar
            
            [1.0.1]: https://jetbrians.com/1.0.1
            """.trimIndent(),
            result.output
        )
    }

    @Test
    fun `returns the Unreleased change notes`() {
        val result = runTask(GET_CHANGELOG_TASK_NAME, "--quiet", "--unreleased")

        assertMarkdown(
            """
            ## [Unreleased]
            Some unreleased changes.
            
            - bar
            
            ### Added
            
            ### Fixed
            - I fixed myself a beverage.
            
            [Unreleased]: https://jetbrians.com/Unreleased
            """.trimIndent(),
            result.output
        )
    }

    fun `returns the Unreleased change notes without empty sections`() {
        val result = runTask(GET_CHANGELOG_TASK_NAME, "--quiet", "--unreleased", "--no-empty-sections")

        assertMarkdown(
            """
            ## [Unreleased]
            Some unreleased changes.
            
            - bar
            
            ### Fixed
            - I fixed myself a beverage.
            
            [Unreleased]: https://jetbrians.com/Unreleased
            """.trimIndent(),
            result.output
        )
    }

    @Test
    fun `returns change notes for the version specified with CLI`() {
        val result = runTask(GET_CHANGELOG_TASK_NAME, "--quiet", "--version=1.0.1")

        assertMarkdown(
            """
            ## [1.0.1] - 2022-10-17
            Release with bugfix.
            
            ### Fixed
            - bar
            
            [1.0.1]: https://jetbrians.com/1.0.1
            """.trimIndent(),
            result.output
        )
    }

    @Test
    fun `returns change notes without header for the latest released version`() {
        val result = runTask(GET_CHANGELOG_TASK_NAME, "--quiet", "--no-header")

        assertMarkdown(
            """
            Release with bugfix.
            
            ### Fixed
            - bar
            
            [1.0.1]: https://jetbrians.com/1.0.1
            """.trimIndent(),
            result.output
        )
    }

    @Test
    fun `returns change notes without summary for the latest released version`() {
        val result = runTask(GET_CHANGELOG_TASK_NAME, "--quiet", "--no-summary")

        assertMarkdown(
            """
            ## [1.0.1] - 2022-10-17
            
            ### Fixed
            - bar
            
            [1.0.1]: https://jetbrians.com/1.0.1
            """.trimIndent(),
            result.output
        )
    }

    @Test
    fun `returns change notes without links for the latest released version`() {
        val result = runTask(GET_CHANGELOG_TASK_NAME, "--quiet", "--no-links")

        assertMarkdown(
            """
            ## 1.0.1 - 2022-10-17
            Release with bugfix.
            
            ### Fixed
            - bar
            """.trimIndent(),
            result.output
        )
    }

    @Test
    fun `returns change notes with summary and links for the latest released version`() {
        val result = runTask(GET_CHANGELOG_TASK_NAME, "--quiet")

        assertMarkdown(
            """
            ## [1.0.1] - 2022-10-17
            Release with bugfix.
            
            ### Fixed
            - bar
            
            [1.0.1]: https://jetbrians.com/1.0.1
            """.trimIndent(),
            result.output
        )
    }

    @Test
    fun `returns change notes with Pattern set to headerParserRegex`() {
        buildFile =
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                headerParserRegex = ~/\d\.\d\.\d/
            }
            """.trimIndent()

        project.evaluate()

        runTask(GET_CHANGELOG_TASK_NAME)
    }

    @Test
    fun `returns change notes with String set to headerParserRegex`() {
        buildFile =
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                headerParserRegex = "\\d\\.\\d\\.\\d"
            }
            """.trimIndent()

        project.evaluate()

        runTask(GET_CHANGELOG_TASK_NAME)
    }

    @Test
    fun `fails with Integer set to headerParserRegex`() {
        buildFile =
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                headerParserRegex = 123
            }
            """.trimIndent()

        project.evaluate()

        runFailingTask(GET_CHANGELOG_TASK_NAME)
    }

    @Test
    fun `task loads from the configuration cache`() {
        runTask(GET_CHANGELOG_TASK_NAME)
        val result = runTask(GET_CHANGELOG_TASK_NAME)

        assertTrue(result.output.contains("Reusing configuration cache."))
    }
}
