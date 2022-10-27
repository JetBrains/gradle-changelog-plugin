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
                repositoryUrl = "https://github.com/JetBrains/gradle-changelog-plugin"
            }
            """.trimIndent()
    }

    @Test
    fun `patches Unreleased version to the current one and creates empty Unreleased above`() {
        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME)

        assertMarkdown(
            """
            ## [1.0.0] - $date
            Fancy release.
            
            ### Added
            - foo
            
            [1.0.0]: https://github.com/JetBrains/gradle-changelog-plugin/tag/v1.0.0
            
            """.trimIndent(),
            extension.renderItem(extension.get(version))
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
            
            [Unreleased]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.0.0...HEAD
            
            """.trimIndent(),
            extension.renderItem(extension.getUnreleased())
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
                repositoryUrl = "https://github.com/JetBrains/gradle-changelog-plugin"
            }
            """.trimIndent()

        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME)

        assertMarkdown(
            """
            ## [1.0.0] - $date
            Fancy release.
            
            ### Added
            - foo
            
            [1.0.0]: https://github.com/JetBrains/gradle-changelog-plugin/tag/v1.0.0
            
            """.trimIndent(),
            extension.renderItem(extension.get(version))
        )

        assertFailsWith<MissingVersionException> {
            extension.getUnreleased().also {
                println("it = ${it}")
            }
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
                repositoryUrl = "https://github.com/JetBrains/gradle-changelog-plugin"
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
                repositoryUrl = "https://github.com/JetBrains/gradle-changelog-plugin"
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
            
            ## [1.0.0] - $date
            Fancy release.
            
            ### Added
            - foo
            
            [Unreleased]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.0.0...HEAD
            [1.0.0]: https://github.com/JetBrains/gradle-changelog-plugin/tag/v1.0.0

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
            
            ## [0.1.0] - 2020-07-02
            
            ### Added
            - Something added.
            
            [Unreleased]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v0.1.0...HEAD
            [0.1.0]: https://github.com/JetBrains/gradle-changelog-plugin/tag/v0.1.0

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
                    "${'$'}{version.get()} - ${'$'}{new SimpleDateFormat("yyyy-MM-dd").format(new Date())}"
                }
                repositoryUrl = "https://github.com/JetBrains/gradle-changelog-plugin"
            }
            """.trimIndent()

        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME)

        val date = SimpleDateFormat("yyyy-MM-dd").format(Date())
        assertEquals("1.0.0 - $date", extension.get(version).header)
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
                repositoryUrl = "https://github.com/JetBrains/gradle-changelog-plugin"
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
            
            [Unreleased]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.0.0...HEAD
            
            """.trimIndent(),
            extension.renderItem(extension.getUnreleased())
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
            ## [1.0.0] - $date
            
            ### Added
            - foo
            
            ### Removed
            - bar
            
            [1.0.0]: https://github.com/JetBrains/gradle-changelog-plugin/tag/v1.0.0
            
            """.trimIndent(),
            extension.renderItem(extension.get(version))
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
                repositoryUrl = "https://github.com/JetBrains/gradle-changelog-plugin"
            }
            """.trimIndent()

        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME)

        assertMarkdown(
            """
            ## [Unreleased]
            
            ### Aaaa
            
            ### Bbb
            
            [Unreleased]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.0.0...HEAD
            
            """.trimIndent(),
            extension.renderItem(extension.getUnreleased())
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
                repositoryUrl = "https://github.com/JetBrains/gradle-changelog-plugin"
            }
            """.trimIndent()

        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME)

        assertMarkdown(
            """
            ## [Unreleased]
            
            [Unreleased]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.0.0...HEAD
            
            """.trimIndent(),
            extension.renderItem(extension.getUnreleased())
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
                repositoryUrl = "https://github.com/JetBrains/gradle-changelog-plugin"
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
                repositoryUrl = "https://github.com/JetBrains/gradle-changelog-plugin"
            }
            """.trimIndent()

        changelog =
            """
            # My Changelog
            Foo bar buz.
            
            ## [Unreleased]
            - Foo
            - Bar
            
            ## [0.1.0] - 2022-10-10
            
            ### Added
            - Buz
            
            [Unreleased]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v0.1.0...HEAD
            [0.1.0]: https://github.com/JetBrains/gradle-changelog-plugin/tag/v0.1.0
            """.trimIndent()

        project.evaluate()

        runTask(PATCH_CHANGELOG_TASK_NAME, "--release-note=Foo\n\n- asd\n- Hello [world]!\n\n[world]: https://jetbrains.com")

        assertMarkdown(
            """
            # My Changelog
            Foo bar buz.
            
            ## [Unreleased]
            
            ## [1.0.0] - $date
            Foo
            
            - asd
            - Hello [world]!
            
            ## [0.1.0] - 2022-10-10
            
            ### Added
            - Buz
            
            [Unreleased]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.0.0...HEAD
            [1.0.0]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v0.1.0...v1.0.0
            [0.1.0]: https://github.com/JetBrains/gradle-changelog-plugin/tag/v0.1.0
            [world]: https://jetbrains.com
            
            """.trimIndent(),
            extension.render()
        )

        assertMarkdown(
            """
            ## [1.0.0] - 2022-10-28
            Foo
            
            - asd
            - Hello [world]!
            
            [1.0.0]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v0.1.0...v1.0.0
            [world]: https://jetbrains.com

            """.trimIndent(),
            extension.renderItem(extension.get("1.0.0"))
        )

        assertMarkdown(
            """
            # My Changelog
            Foo bar buz.
            
            ## [Unreleased]
            
            ## [1.0.0] - 2022-10-28
            Foo
            
            - asd
            - Hello [world]!
            
            ## [0.1.0] - 2022-10-10
            
            ### Added
            - Buz
            
            [Unreleased]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.0.0...HEAD
            [1.0.0]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v0.1.0...v1.0.0
            [0.1.0]: https://github.com/JetBrains/gradle-changelog-plugin/tag/v0.1.0
            [world]: https://jetbrains.com
            
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
            
            ## [0.0.1] - 2022-10-10
            
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
            
            ## [1.0.0] - $date
            
            ### Changed
            - Update some feature
            
            ## [0.0.1] - 2022-10-10
            
            ### Added
            - `RunBlocking` function
            
            [Unreleased]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.0.0...HEAD
            [1.0.0]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v0.0.1...v1.0.0
            [0.0.1]: https://github.com/JetBrains/gradle-changelog-plugin/tag/v0.0.1
            
            """.trimIndent(),
            extension.render()
        )
    }

    @Test
    fun `keep an existing changelog title`() {
        changelog =
            """
            # My Changelog
            
            ## Unreleased
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
                repositoryUrl = "https://github.com/JetBrains/gradle-changelog-plugin"
            }
            """.trimIndent()

        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME)

        assertMarkdown(
            """
            # My Changelog
            
            ## [1.0.0] - $date
            - Fixed stuff
            
            [1.0.0]: https://github.com/JetBrains/gradle-changelog-plugin/tag/v1.0.0
            """.trimIndent(),
            extension.render()
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
                repositoryUrl = "https://github.com/JetBrains/gradle-changelog-plugin"
            }
            """.trimIndent()

        project.evaluate()
        runTask(PATCH_CHANGELOG_TASK_NAME)

        assertMarkdown(
            """
            # Project Changelog
            
            ## [1.0.0] - $date
            - Fixed stuff
            
            [1.0.0]: https://github.com/JetBrains/gradle-changelog-plugin/tag/v1.0.0

            """.trimIndent(),
            extension.render()
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
            
            ## [1.0.0-beta] - 2022-10-13
            A really awesome release.
            
            ### Added
            - Feature
            
            ### Fixed
            - Another bug
            
            ### Removed
            - Unnecessary file
            
            ## [1.0.0-alpha.2] - 2022-10-12
            An awesome release.
            
            ### Fixed
            - Bug fix
            
            ## [1.0.0-alpha.1] - 2022-10-11
            An awesome release.
            
            ### Fixed
            - Bug fix
            
            ## [0.9.0] - 2022-10-10
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
            
            ## [1.0.0] - $date
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
            
            ## [1.0.0-beta] - 2022-10-13
            A really awesome release.
            
            ### Added
            - Feature
            
            ### Fixed
            - Another bug
            
            ### Removed
            - Unnecessary file
            
            ## [1.0.0-alpha.2] - 2022-10-12
            An awesome release.
            
            ### Fixed
            - Bug fix
            
            ## [1.0.0-alpha.1] - 2022-10-11
            An awesome release.
            
            ### Fixed
            - Bug fix
            
            ## [0.9.0] - 2022-10-10
            An old release
            
            ### Added
            - Some additional feature
            
            [Unreleased]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.0.0...HEAD
            [1.0.0]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.0.0-beta...v1.0.0
            [1.0.0-beta]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.0.0-alpha.2...v1.0.0-beta
            [1.0.0-alpha.2]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.0.0-alpha.1...v1.0.0-alpha.2
            [1.0.0-alpha.1]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v0.9.0...v1.0.0-alpha.1
            [0.9.0]: https://github.com/JetBrains/gradle-changelog-plugin/tag/v0.9.0
            
            """.trimIndent(),
            extension.render()
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
                repositoryUrl = "https://github.com/JetBrains/gradle-changelog-plugin"
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
            
            ## [1.0.0-beta] - 2022-10-13
            A really awesome release.
            
            ### Added
            - Feature
            
            ### Fixed
            - Another bug
            
            ### Removed
            - Unnecessary file
            
            ## [1.0.0-alpha.2] - 2022-10-12
            An awesome release.
            
            ### Fixed
            - Bug fix
            
            ## [1.0.0-alpha.1] - 2022-10-11
            An awesome release.
            
            ### Fixed
            - Bug fix
            
            ## [0.9.0] - 2022-10-10
            An old release
            
            ### Added
            - Some additional feature
            
            [Unreleased]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.0.0...HEAD
            [1.0.0]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.0.0-beta...v1.0.0
            [1.0.0-beta]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.0.0-alpha.2...v1.0.0-beta
            [1.0.0-alpha.2]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.0.0-alpha.1...v1.0.0-alpha.2
            [1.0.0-alpha.1]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v0.9.0...v1.0.0-alpha.1
            [0.9.0]: https://github.com/JetBrains/gradle-changelog-plugin/tag/v0.9.0
            
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
            
            ## [1.0.0] - $date
            
            ### Added
            - New feature
            
            ### Changed
            - Changes
            
            ## [1.0.0-beta] - 2022-10-13
            A really awesome release.
            
            ### Added
            - Feature
            
            ### Fixed
            - Another bug
            
            ### Removed
            - Unnecessary file
            
            ## [1.0.0-alpha.2] - 2022-10-12
            An awesome release.
            
            ### Fixed
            - Bug fix
            
            ## [1.0.0-alpha.1] - 2022-10-11
            An awesome release.
            
            ### Fixed
            - Bug fix
            
            ## [0.9.0] - 2022-10-10
            An old release
            
            ### Added
            - Some additional feature
            
            [Unreleased]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.0.0...HEAD
            [1.0.0]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.0.0-beta...v1.0.0
            [1.0.0-beta]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.0.0-alpha.2...v1.0.0-beta
            [1.0.0-alpha.2]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.0.0-alpha.1...v1.0.0-alpha.2
            [1.0.0-alpha.1]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v0.9.0...v1.0.0-alpha.1
            [0.9.0]: https://github.com/JetBrains/gradle-changelog-plugin/tag/v0.9.0
            
            """.trimIndent(),
            extension.render()
        )
    }

    @Test
    fun `task loads from the configuration cache`() {
        runTask(PATCH_CHANGELOG_TASK_NAME)
        val result = runTask(PATCH_CHANGELOG_TASK_NAME)

        assertTrue(result.output.contains("Reusing configuration cache."))
    }
}
