package org.jetbrains.changelog.tasks

import java.text.SimpleDateFormat
import java.util.Date
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.jetbrains.changelog.BaseTest
import org.jetbrains.changelog.exceptions.MissingVersionException

class PatchChangelogTaskTest : BaseTest() {

    @BeforeTest
    fun localSetUp() {
        version = "1.0.0"
        changelog = """
            # Changelog
            ## [Unreleased]
            ### Added
            - foo
        """

        buildFile = """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
            }
        """
    }

    @Test
    fun `patches Unreleased version to the current one and creates empty Unreleased above`() {
        project.evaluate()
        runTask("patchChangelog")

        assertEquals("""
            ### Added
            - foo
        """.trimIndent(), extension.get().toText())

        assertEquals("""
            ## [Unreleased]
            ### Added
            
            ### Changed
            
            ### Deprecated
            
            ### Removed
            
            ### Fixed
            
            ### Security
        """.trimIndent(), extension.getUnreleased().withHeader(true).toText())
    }

    @Test
    fun `patches Unreleased version to the current one`() {
        buildFile = """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                keepUnreleasedSection = false
            }
        """

        project.evaluate()
        runTask("patchChangelog")

        assertEquals("""
            ### Added
            - foo
        """.trimIndent(), extension.get().toText())

        assertFailsWith<MissingVersionException> {
            extension.getUnreleased()
        }
    }

    @Test
    fun `applies custom header patcher`() {
        buildFile = """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                
                headerFormat = "Foo {0} bar {1}"
                headerArguments = ["${project.version}", "buz"]
            }
        """
        extension.headerFormat = "Foo {0} bar {1}"

        project.evaluate()
        runTask("patchChangelog")

        assertEquals("## Foo 1.0.0 bar buz", extension.get().getHeader())
    }

    @Test
    fun `applies custom header with date`() {
        changelog = """
            # Changelog
            All notable changes to this project will be documented in this file.

            ## [Unreleased]
            ### Added
            - Some other thing added.

            ## [1.0.0] - 2020-07-02

            ### Added
            - Something added.
        """
        buildFile = """
            import java.text.SimpleDateFormat
            import java.util.Arrays
            import java.util.Date

            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                
                headerFormat = "[{0}] - {1}"
                headerArguments = Arrays.asList(version, new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
            }
        """
        extension.headerFormat = "[{0}] - {1}"

        project.evaluate()
        runTask("patchChangelog")

        val date = SimpleDateFormat("yyyy-MM-dd").format(Date())
        assertEquals("## [1.0.0] - $date", extension.get().getHeader())
    }

    @Test
    fun `doesn't patch changelog if no change notes provided in Unreleased section`() {
        changelog = """
            # Changelog
            ## [Unreleased]
        """
        buildFile = """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                patchEmpty = false
            }
        """

        project.evaluate()

        runTask("patchChangelog")

        assertFailsWith<MissingVersionException> {
            extension.get()
        }
    }

    @Test
    fun `Create empty groups for the new Unreleased section`() {
        project.evaluate()
        runTask("patchChangelog")

        assertEquals("""
            ## [Unreleased]
            ### Added
            
            ### Changed
            
            ### Deprecated
            
            ### Removed
            
            ### Fixed
            
            ### Security
        """.trimIndent(), extension.getUnreleased().withHeader(true).toText())
    }

    @Test
    fun `Remove empty groups for the new released section`() {
        changelog = """
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
        """

        project.evaluate()
        runTask("patchChangelog")

        assertEquals("""
            ## [1.0.0]
            ### Added
            - foo
            
            ### Removed
            - bar
        """.trimIndent(), extension.get().withHeader(true).toText())
    }

    @Test
    fun `Create empty custom groups for the new Unreleased section`() {
        buildFile = """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                groups = ["Aaaa", "Bbb"]
            }
        """

        project.evaluate()
        runTask("patchChangelog")

        assertEquals("""
            ## [Unreleased]
            ### Aaaa

            ### Bbb
        """.trimIndent(), extension.getUnreleased().withHeader(true).toText())
    }
}
