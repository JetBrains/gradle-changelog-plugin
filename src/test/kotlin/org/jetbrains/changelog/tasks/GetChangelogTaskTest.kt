// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.tasks

import org.jetbrains.changelog.BaseTest
import org.jetbrains.changelog.ChangelogPluginConstants.GET_CHANGELOG_TASK_NAME
import org.jetbrains.changelog.normalizeLineSeparator
import java.io.File
import kotlin.io.path.Path
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

    @Test
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
        val result = runTask(GET_CHANGELOG_TASK_NAME, "--quiet", "--project-version=1.0.1")

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

    @Test
    fun `get changelog with CRLF line separator`() {
        //language=Markdown
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
            """.trimIndent().normalizeLineSeparator("\r\n")

        val result = runTask(GET_CHANGELOG_TASK_NAME, "--quiet")

        //language=Markdown
        assertMarkdown(
            """
            ## [1.0.1] - 2022-10-17
            
            Release with bugfix.
            
            ### Fixed
            
            - bar
            
            [1.0.1]: https://jetbrians.com/1.0.1
            """.trimIndent().normalizeLineSeparator("\r\n"),
            result.output
        )
    }

    @Test
    fun `get changelog with CR line separator`() {
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
            """.trimIndent().normalizeLineSeparator("\r")

        val result = runTask(GET_CHANGELOG_TASK_NAME, "--quiet")

        //language=Markdown
        assertMarkdown(
            """
            ## [1.0.1] - 2022-10-17
            
            Release with bugfix.
            
            ### Fixed
            
            - bar
            
            [1.0.1]: https://jetbrians.com/1.0.1
            """.trimIndent().normalizeLineSeparator("\r"),
            result.output
        )
    }

    @Test
    fun `writes changelog to a file for the latest released version`() {
        val outputFile = File(project.projectDir, "latest-changelog.md")
        
        val result = runTask(GET_CHANGELOG_TASK_NAME, "--quiet", "--output-file=${outputFile.absolutePath}")

        // Check that the file was created
        assertTrue(outputFile.exists(), "Output file should exist")
        
        // Check the file content
        assertMarkdown(
            """
            ## [1.0.1] - 2022-10-17
            
            Release with bugfix.
            
            ### Fixed
            
            - bar
            
            [1.0.1]: https://jetbrians.com/1.0.1
            """.trimIndent(),
            outputFile.readText()
        )
        
        // Check that the output is NOT logged to console when writing to a file
        assertTrue(result.output.trim().isEmpty() || 
                  !result.output.contains("[1.0.1]") && 
                  !result.output.contains("Release with bugfix."),
                  "Output should not contain changelog content when writing to a file")
    }

    @Test
    fun `writes changelog to a file for the unreleased version`() {
        val outputFile = File(project.projectDir, "unreleased-changelog.md")
        
        runTask(GET_CHANGELOG_TASK_NAME, "--quiet", "--unreleased", "--output-file=${outputFile.absolutePath}")

        // Check that the file was created
        assertTrue(outputFile.exists(), "Output file should exist")
        
        // Check the file content
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
            outputFile.readText()
        )
    }

    @Test
    fun `writes changelog to a file with formatting options`() {
        val outputFile = File(project.projectDir, "formatted-changelog.md")
        
        runTask(
            GET_CHANGELOG_TASK_NAME, 
            "--quiet", 
            "--no-header", 
            "--no-summary", 
            "--no-links", 
            "--output-file=${outputFile.absolutePath}"
        )

        // Check the file content
        assertMarkdown(
            """
            ### Fixed
            
            - bar
            """.trimIndent(),
            outputFile.readText()
        )
    }

    @Test
    fun `writes changelog to a file in a non-existent directory`() {
        val outputDir = File(project.projectDir, "output/nested/dir")
        val outputFile = File(outputDir, "changelog.md")
        
        runTask(GET_CHANGELOG_TASK_NAME, "--quiet", "--output-file=${outputFile.absolutePath}")

        // Check that the directory and file were created
        assertTrue(outputDir.exists(), "Output directory should exist")
        assertTrue(outputFile.exists(), "Output file should exist")
        
        // Check the file content
        assertMarkdown(
            """
            ## [1.0.1] - 2022-10-17
            
            Release with bugfix.
            
            ### Fixed
            
            - bar
            
            [1.0.1]: https://jetbrians.com/1.0.1
            """.trimIndent(),
            outputFile.readText()
        )
    }
    
    @Test
    fun `logs changelog to console when no output file is provided`() {
        val result = runTask(GET_CHANGELOG_TASK_NAME, "--quiet")
        
        // Check that the output is logged to console when no output file is provided
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
    fun `uses output file from extension configuration`() {
        // Set up the extension with an output file
        val outputFile = project.file("extension-changelog.md")
        buildFile =
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                outputFile = file("${outputFile.invariantSeparatorsPath}")
            }
            """.trimIndent()
        
        project.evaluate()
        
        // Run the task without specifying an output file on the command line
        val result = runTask(GET_CHANGELOG_TASK_NAME, "--quiet")
        
        // Check that the file was created
        assertTrue(outputFile.exists(), "Output file should exist")
        
        // Check the file content
        assertMarkdown(
            """
            ## [1.0.1] - 2022-10-17
            
            Release with bugfix.
            
            ### Fixed
            
            - bar
            
            [1.0.1]: https://jetbrians.com/1.0.1
            """.trimIndent(),
            outputFile.readText()
        )
        
        // Check that the output is NOT logged to console when writing to a file
        assertTrue(result.output.trim().isEmpty() || 
                  !result.output.contains("[1.0.1]") && 
                  !result.output.contains("Release with bugfix."),
                  "Output should not contain changelog content when writing to a file")
    }
}
