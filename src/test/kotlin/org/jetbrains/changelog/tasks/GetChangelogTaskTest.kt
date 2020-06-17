package org.jetbrains.changelog.tasks

import org.jetbrains.changelog.BaseTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetChangelogTaskTest : BaseTest() {

    @BeforeTest
    fun localSetUp() {
        version = "1.0.0"
        changelog = """
            # Changelog
            ## Unreleased
            - bar
            ## [1.0.0]
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

        project.evaluate()
    }

    @Test
    fun `returns change notes for the version specified with extension`() {
        val result = runTask("getChangelog")

        assertEquals("""
            ## [1.0.0]
            ### Added
            - foo
        """.trimIndent(), result.output.trim())
    }

    @Test
    fun `returns the Unreleased change notes`() {
        val result = runTask("getChangelog", "--unreleased")

        assertEquals("""
            ## Unreleased
            - bar
        """.trimIndent(), result.output.trim())
    }

    @Test
    fun `returns change notes without header for the version specified with extension`() {
        val result = runTask("getChangelog", "--no-header")

        assertEquals("""
            ### Added
            - foo
        """.trimIndent(), result.output.trim())
    }
}
