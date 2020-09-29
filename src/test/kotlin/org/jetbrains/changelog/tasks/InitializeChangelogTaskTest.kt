package org.jetbrains.changelog.tasks

import org.jetbrains.changelog.BaseTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class InitializeChangelogTaskTest : BaseTest() {

    @BeforeTest
    fun localSetUp() {
        version = "1.0.0"
        buildFile =
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
            }
            """

        project.evaluate()
    }

    @Test
    fun `creates new changelog file`() {
        runTask("initializeChangelog")

        extension.getAll().apply {
            assertEquals(1, keys.size)
            assertEquals("[Unreleased]", keys.first())
            assertEquals(
                """
                ## [Unreleased]
                ### Added
                - Example item

                ### Changed

                ### Deprecated

                ### Removed

                ### Fixed

                ### Security
                """.trimIndent(),
                values.first().withHeader(true).toText()
            )
        }

        assertNotNull(extension.getUnreleased())
    }

    @Test
    fun `overrides existing changelog file`() {
        changelog =
            """
            # Changelog
            """
        project.evaluate()

        runTask("initializeChangelog")

        assertEquals(
            """
            ## [Unreleased]
            ### Added
            - Example item

            ### Changed

            ### Deprecated

            ### Removed

            ### Fixed

            ### Security
            """.trimIndent(),
            extension.getUnreleased().withHeader(true).toText()
        )
    }

    @Test
    fun `creates customized changelog file`() {
        buildFile =
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                version = "1.0.0"
                path = "${project.projectDir}/CHANGES.md"
                itemPrefix = "*"
                unreleasedTerm = "Upcoming version"
                groups = ["Added", "Removed"]
            }
            """
        extension.apply {
            path = "${project.projectDir}/CHANGES.md"
            unreleasedTerm = "Upcoming version"
            itemPrefix = "*"
        }
        project.evaluate()

        runTask("initializeChangelog")

        assertEquals(
            """
            ## Upcoming version
            ### Added
            * Example item
            
            ### Removed
            """.trimIndent(),
            extension.getUnreleased().withHeader(true).toText()
        )
    }
}
