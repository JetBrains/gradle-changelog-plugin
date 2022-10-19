// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.tasks

import org.jetbrains.changelog.BaseTest
import org.jetbrains.changelog.ChangelogPluginConstants.PATCH_CHANGELOG_TASK_NAME
import org.jetbrains.changelog.exceptions.MissingVersionException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.test.*

class PatchChangelogTaskTest : BaseTest() {

    @BeforeTest
    fun localSetUp() {
        changelog = //language=markdown
            """
            <!-- Foo bar -->
            
            # Changelog
            My project changelog.
            
            ## [Unreleased]
            Fancy release.
            
            ### Added
            - foo
            """.trimIndent()

        buildFile = //language=Groovy
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
            }
            """.trimIndent()
    }

    @Test
    fun `patches Unreleased version to the current one and creates empty Unreleased above`() {
        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME)

        //language=markdown
        assertEquals(
            """
            ## [1.0.0]
            Fancy release.
            
            ### Added
            - foo
            
            """.trimIndent(),
            extension.get(version).toText()
        )

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
    fun `patches Unreleased version to the current one`() {
        buildFile = //language=Groovy
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "$version"
                keepUnreleasedSection = false
            }
            """.trimIndent()

        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME)

        //language=markdown
        assertEquals(
            """
            ## [$version]
            Fancy release.
            
            ### Added
            - foo
            
            """.trimIndent(),
            extension.get(version).toText()
        )

        assertFailsWith<MissingVersionException> {
            extension.getUnreleased()
        }
    }

    @Test
    fun `applies custom header patcher`() {
        buildFile = //language=Groovy
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "$version"
                header = provider { "Foo ${'$'}{version.get()} bar" }
            }
            """.trimIndent()

        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME)

        assertEquals("## Foo 1.0.0 bar", extension.get(version).header)
    }

    @Test
    fun `applies custom introduction`() {
        buildFile = //language=Groovy
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                introduction = "Foo bar"
            }
            """.trimIndent()

        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME)

        //language=markdown
        assertEquals(
            """
            <!-- Foo bar -->
            
            # Changelog
            Foo bar
            
            ## [Unreleased]
            
            ### Added
            
            ### Changed
            
            ### Deprecated
            
            ### Removed
            
            ### Fixed
            
            ### Security
            
            ## [1.0.0]
            Fancy release.
            
            ### Added
            - foo

            """.trimIndent(),
            changelog,
        )
    }

    @Test
    fun `applies custom header with date`() {
        changelog = //language=markdown
            """
            # Changelog
            All notable changes to this project will be documented in this file.

            ## [Unreleased]
            ### Added
            - Some other thing added.

            ## [1.0.0] - 2020-07-02

            ### Added
            - Something added.
            """.trimIndent()

        buildFile = //language=Groovy
            """
            import java.text.SimpleDateFormat
            import java.util.Date

            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                header = provider {
                    "[${'$'}{version.get()}] - ${'$'}{new SimpleDateFormat("yyyy-MM-dd").format(new Date())}"
                }
            }
            """.trimIndent()

        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME)

        val date = SimpleDateFormat("yyyy-MM-dd").format(Date())
        assertEquals("## [1.0.0] - $date", extension.get(version).header)
    }

    @Test
    fun `doesn't patch changelog if no change notes provided in Unreleased section`() {
        changelog = //language=markdown
            """
            # Changelog
            ## [Unreleased]
            """.trimIndent()

        buildFile = //language=Groovy
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                patchEmpty = false
            }
            """.trimIndent()

        project.evaluate()

        val result = runFailingTask(PATCH_CHANGELOG_TASK_NAME)

        assertFailsWith<MissingVersionException> {
            extension.get(version)
        }

        assertTrue(result.output.contains(":$PATCH_CHANGELOG_TASK_NAME task requires release note to be provided."))
    }

    @Test
    fun `create empty groups for the new Unreleased section`() {
        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME)

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
    fun `remove empty groups for the new released section`() {
        changelog = //language=markdown
            """
            # Changelog
            ## [Unreleased]
            ### Added
            - foo
            
            ### Changed
            
            ### Deprecated
            
            ### Removed
            - bar
            
            ### Fixed
            
            ### Security
            """.trimIndent()

        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME)

        //language=markdown
        assertEquals(
            """
            ## [1.0.0]

            ### Added
            - foo
            
            ### Removed
            - bar
            
            """.trimIndent(),
            extension.get(version).toText()
        )
    }

    @Test
    fun `create empty custom groups for the new Unreleased section`() {
        buildFile = //language=Groovy
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                groups = ["Aaaa", "Bbb"]
            }
            """.trimIndent()

        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME)

        //language=markdown
        assertEquals(
            """
            ## [Unreleased]

            ### Aaaa

            ### Bbb
            
            """.trimIndent(),
            extension.getUnreleased().toText()
        )
    }

    @Test
    fun `don't create groups for the new Unreleased section if empty array is provided`() {
        buildFile = //language=Groovy
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                groups = []
            }
            """.trimIndent()

        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME)

        //language=markdown
        assertEquals(
            """
            ## [Unreleased]
            
            """.trimIndent(),
            extension.getUnreleased().toText()
        )
    }

    @Test
    fun `throws MissingReleaseNoteException when Unreleased section is not present`() {
        val unreleasedTerm = "Not released"
        buildFile = //language=Groovy
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                unreleasedTerm = "$unreleasedTerm"
            }
            """.trimIndent()

        changelog = //language=markdown
            """
            ## [1.0.0]
            """.trimIndent()

        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME, "--warn")

        assertFailsWith<MissingVersionException> {
            extension.getUnreleased()
        }
    }

    @Test
    fun `throws VersionNotSpecifiedException when changelog extension has no version provided`() {
        buildFile = //language=Groovy
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            """.trimIndent()

        project.evaluate()

        val result = runFailingTask(PATCH_CHANGELOG_TASK_NAME)

        assertTrue(
            result.output.contains(
                "org.jetbrains.changelog.exceptions.VersionNotSpecifiedException: Version is missing. " +
                        "Please provide the project version to the `project` or `changelog.version` property explicitly."
            )
        )
    }

    @Test
    fun `patch changelog with a custom release note`() {
        buildFile = //language=Groovy
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                groups = []
            }
            """.trimIndent()

        changelog = //language=markdown
            """
            # My Changelog
            Foo bar buz.
            
            ## [Unreleased]
            - Foo
            - Bar
            
            ## [0.1.0]
            
            ### Added
            - Buz
            """.trimIndent()

        project.evaluate()

        runTask(PATCH_CHANGELOG_TASK_NAME, "--release-note=- asd")

        //language=markdown
        assertEquals(
            """
            ## [1.0.0]
            - asd

            """.trimIndent(),
            extension.get("1.0.0").toText()
        )

        //language=markdown
        assertEquals(
            """
            # Changelog
            Foo bar buz.
            
            ## [Unreleased]
            
            ## [1.0.0]
            - asd

            ## [0.1.0]
            
            ### Added
            - Buz
            
            """.trimIndent(),
            changelog,
        )

        assertFalse(changelog.endsWith(lineSeparator + lineSeparator))
        assertTrue(changelog.endsWith(lineSeparator))
    }

    @Test
    fun `patched changelog contains an empty line at the end`() {
        runTask(PATCH_CHANGELOG_TASK_NAME)

        assertTrue(changelog.endsWith(lineSeparator))
    }

    @Test
    fun `task loads from the configuration cache`() {
        runTask(PATCH_CHANGELOG_TASK_NAME)
        val result = runTask(PATCH_CHANGELOG_TASK_NAME)

        assertTrue(result.output.contains("Reusing configuration cache."))
    }
}
