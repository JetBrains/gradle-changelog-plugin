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
        changelog =
            """
            <!-- Foo bar -->
            
            # Changelog
            My project changelog.
            
            ## [Unreleased]
            Fancy release.
            
            ### Added
            - foo
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
    }

    @Test
    fun `patches Unreleased version to the current one and creates empty Unreleased above`() {
        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME)

        assertMarkdown(
            """
            ## [1.0.0]
            Fancy release.
            
            ### Added
            - foo
            
            """.trimIndent(),
            extension.get(version).toText()
        )

        assertMarkdown(
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
        buildFile =
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

        assertMarkdown(
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
        buildFile =
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

        assertEquals("Foo 1.0.0 bar", extension.get(version).header)
    }

    @Test
    fun `applies custom introduction`() {
        buildFile =
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

        assertMarkdown(
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
        changelog =
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

        buildFile =
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
        assertEquals("[1.0.0] - $date", extension.get(version).header)
    }

    @Test
    fun `doesn't patch changelog if no change notes provided in Unreleased section`() {
        changelog =
            """
            # Changelog
            ## [Unreleased]
            """.trimIndent()

        buildFile =
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

        assertMarkdown(
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
        changelog =
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

        assertMarkdown(
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
        buildFile =
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

        assertMarkdown(
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
        buildFile =
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

        assertMarkdown(
            """
            ## [Unreleased]
            
            """.trimIndent(),
            extension.getUnreleased().toText()
        )
    }

    @Test
    fun `throws MissingReleaseNoteException when Unreleased section is not present`() {
        val unreleasedTerm = "Not released"

        buildFile =
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                unreleasedTerm = "$unreleasedTerm"
                keepUnreleasedSection = false
            }
            """.trimIndent()

        changelog =
            """
            ## $unreleasedTerm
            - foo
            """.trimIndent()

        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME, "--warn")

        assertFailsWith<MissingVersionException> {
            extension.getUnreleased()
        }
    }

    @Test
    fun `throws VersionNotSpecifiedException when changelog extension has no version provided`() {
        buildFile =
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
        buildFile =
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                groups = []
            }
            """.trimIndent()

        changelog =
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

        assertMarkdown(
            """
            ## [1.0.0]
            - asd

            """.trimIndent(),
            extension.get("1.0.0").toText()
        )

        assertMarkdown(
            """
            # My Changelog
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
    fun `removes empty groups`() {
        changelog =
            """
            # Changelog
            
            ## [Unreleased]
            
            ### Changed
            
            - Update some feature
            
            ### Removed
            
            ### Fixed
            ## [0.0.1]
            
            ### Added
            
            - `RunBlocking` function
            
            ### Changed
            
            ### Deprecated
            
            ### Removed
            
            ### Fixed
            
            ### Updated
            """.trimIndent()

        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME)

        assertMarkdown(
            """
            # Changelog
            
            ## [Unreleased]
            
            ### Added
            
            ### Changed
            
            ### Deprecated
            
            ### Removed
            
            ### Fixed
            
            ### Security
            
            ## [1.0.0]
            
            ### Changed
            - Update some feature
            
            ## [0.0.1]
            
            ### Added
            - `RunBlocking` function
            
            """.trimIndent(),
            extension.instance.content
        )
    }

    @Test
    fun `keep an existing changelog title`() {
        changelog =
            """
            # My Changelog
            
            ## [Unreleased]
            - Fixed stuff
            """.trimIndent()

        buildFile =
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                keepUnreleasedSection = false
            }
            """.trimIndent()

        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME)

        assertMarkdown(
            """
            # My Changelog
            
            ## [1.0.0]
            - Fixed stuff

            """.trimIndent(),
            extension.instance.content
        )
    }

    @Test
    fun `update changelog title`() {
        changelog =
            """
            # My Changelog
            
            ## [Unreleased]
            - Fixed stuff
            """.trimIndent()

        buildFile =
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                keepUnreleasedSection = false
                title = "Project Changelog"
            }
            """.trimIndent()

        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME)

        assertMarkdown(
            """
            # Project Changelog
            
            ## [1.0.0]
            - Fixed stuff

            """.trimIndent(),
            extension.instance.content
        )
    }

    @Test
    fun `combine pre-releases`() {
        changelog =
            """
            # My Changelog
            
            ## [Unreleased]
            
            ### Added
            - New feature
            
            ### Changed
            - Changes
            
            ## [1.0.0-beta]
            A really awesome release.
            
            ### Added
            - Feature
            
            ### Fixed
            - Another bug
            
            ### Removed
            - Unnecessary file
            
            ## [1.0.0-alpha.2]
            An awesome release.
            
            ### Fixed
            - Bug fix
            
            ## [1.0.0-alpha.1]
            An awesome release.
            
            ### Fixed
            - Bug fix
            
            ## [1.9.0]
            An old release
            
            ### Added
            - Some additional feature
            
            """.trimIndent()

        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME)

        assertMarkdown(
            """
            # My Changelog
            
            ## [Unreleased]
            
            ### Added
            
            ### Changed
            
            ### Deprecated
            
            ### Removed
            
            ### Fixed
            
            ### Security
            
            ## [1.0.0]
            A really awesome release.
            
            ### Added
            - New feature
            - Feature
            
            ### Changed
            - Changes
            
            ### Fixed
            - Another bug
            - Bug fix
            
            ### Removed
            - Unnecessary file
            
            ## [1.0.0-beta]
            A really awesome release.
            
            ### Added
            - Feature
            
            ### Fixed
            - Another bug
            
            ### Removed
            - Unnecessary file
            
            ## [1.0.0-alpha.2]
            An awesome release.
            
            ### Fixed
            - Bug fix
            
            ## [1.0.0-alpha.1]
            An awesome release.
            
            ### Fixed
            - Bug fix
            
            ## [1.9.0]
            An old release
            
            ### Added
            - Some additional feature
            
            """.trimIndent(),
            extension.instance.content
        )
    }

    @Test
    fun `do not combine pre-releases`() {
        buildFile =
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                combinePreReleases = false
            }
            """.trimIndent()
        changelog =
            """
            # My Changelog
            
            ## [Unreleased]
            
            ### Added
            - New feature
            
            ### Changed
            - Changes
            
            ## [1.0.0-beta]
            A really awesome release.
            
            ### Added
            - Feature
            
            ### Fixed
            - Another bug
            
            ### Removed
            - Unnecessary file
            
            ## [1.0.0-alpha.2]
            An awesome release.
            
            ### Fixed
            - Bug fix
            
            ## [1.0.0-alpha.1]
            An awesome release.
            
            ### Fixed
            - Bug fix
            
            ## [1.9.0]
            An old release
            
            ### Added
            - Some additional feature
            
            """.trimIndent()

        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME)

        assertMarkdown(
            """
            # My Changelog
            
            ## [Unreleased]
            
            ### Added
            
            ### Changed
            
            ### Deprecated
            
            ### Removed
            
            ### Fixed
            
            ### Security
            
            ## [1.0.0]
            
            ### Added
            - New feature
            
            ### Changed
            - Changes
            
            ## [1.0.0-beta]
            A really awesome release.
            
            ### Added
            - Feature
            
            ### Fixed
            - Another bug
            
            ### Removed
            - Unnecessary file
            
            ## [1.0.0-alpha.2]
            An awesome release.
            
            ### Fixed
            - Bug fix
            
            ## [1.0.0-alpha.1]
            An awesome release.
            
            ### Fixed
            - Bug fix
            
            ## [1.9.0]
            An old release
            
            ### Added
            - Some additional feature
            
            """.trimIndent(),
            extension.instance.content
        )
    }

    @Test
    fun `task loads from the configuration cache`() {
        runTask(PATCH_CHANGELOG_TASK_NAME)
        val result = runTask(PATCH_CHANGELOG_TASK_NAME)

        assertTrue(result.output.contains("Reusing configuration cache."))
    }
}
