package org.jetbrains.changelog.tasks

import org.jetbrains.changelog.BaseTest
import org.jetbrains.changelog.exceptions.MissingVersionException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PatchChangelogTaskTest : BaseTest() {

    @BeforeTest
    fun localSetUp() {
        version = "1.0.0"
        changelog = """
            # Changelog
            ## Unreleased
            ### Added
            - foo
        """.trimIndent()

        buildFile = """
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
        runTask("patchChangelog")

        assertEquals("""
            ### Added
            - foo
        """.trimIndent(), extension.get().toText())

        assertEquals("""
            ## Unreleased
            
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
        """.trimIndent()

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
        """.trimIndent()
        extension.headerFormat = "Foo {0} bar {1}"

        project.evaluate()
        runTask("patchChangelog")

        assertEquals("## Foo 1.0.0 bar buz", extension.get().getHeader())
    }

    @Test
    fun `doesn't patch changelog if no change notes provided in Unreleased section`() {
        changelog = """
            # Changelog
            ## Unreleased
        """.trimIndent()
        buildFile = """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                patchEmpty = false
            }
        """.trimIndent()

        project.evaluate()

        runTask("patchChangelog")

        assertFailsWith<MissingVersionException> {
            extension.get()
        }
    }
}
