package org.jetbrains.changelog.tasks

import org.jetbrains.changelog.BaseTest
import org.jetbrains.changelog.exceptions.MissingVersionException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PatchChangelogTaskTest : BaseTest() {

    @BeforeTest
    fun taskSetUp() {
        version = "1.0.0"
        changelog = """
            # Changelog
            ## [Unreleased]
            foo
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
    fun `Patches Unreleased version to the current one and creates empty Unreleased above`() {
        project.evaluate()
        runTask("patchChangelog")

        assertEquals("""
            ## [1.0.0]
            foo
        """.trimIndent(), extension.get().toText())

        assertEquals("""
            ## [Unreleased]
        """.trimIndent(), extension.getUnreleased().toText())
    }

    @Test
    fun `Patches Unreleased version to the current one`() {
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
            ## [1.0.0]
            foo
        """.trimIndent(), extension.get().toText())

        assertFailsWith<MissingVersionException> {
            extension.getUnreleased()
        }
    }
}
