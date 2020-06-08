package org.jetbrains.changelog.tasks

import org.jetbrains.changelog.BaseTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetChangelogTaskTest : BaseTest() {

    @BeforeTest
    fun taskSetUp() {
        version = "1.0.0"
        changelog = """
            # Changelog
            ## [1.0.0]
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

        project.evaluate()
    }

    @Test
    fun `Returns change notes for the version specified with extension`() {
        val result = runTask("getChangelog")

        assertEquals("""
            ## [1.0.0]
            foo
        """.trimIndent(), result.output.trim())
    }

    @Test
    fun `Returns change notes without header for the version specified with extension`() {
        val result = runTask("getChangelog", "--no-header")

        assertEquals("""
            foo
        """.trimIndent(), result.output.trim())
    }
}
