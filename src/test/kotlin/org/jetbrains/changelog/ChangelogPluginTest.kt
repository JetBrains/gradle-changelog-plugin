package org.jetbrains.changelog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.jetbrains.changelog.tasks.GetChangelogTask
import org.jetbrains.changelog.tasks.InitializeChangelogTask
import org.jetbrains.changelog.tasks.PatchChangelogTask

class ChangelogPluginTest : BaseTest() {

    @Test
    fun `default properties values`() {
        assertNotNull(extension)
        assertEquals("[{0}]", extension.headerFormat)
        assertEquals("[{0}]", extension.headerMessageFormat().toPattern())
        assertTrue(extension.keepUnreleasedSection)
        assertEquals("${project.projectDir}/CHANGELOG.md", extension.path)
        assertEquals(project.version, extension.version)
        assertEquals("[Unreleased]", extension.unreleasedTerm)
    }

    @Test
    fun `tasks availability`() {
        (project.tasks.findByName("initializeChangelog") as InitializeChangelogTask).apply {
            assertNotNull(this)
        }

        (project.tasks.findByName("getChangelog") as GetChangelogTask).apply {
            assertNotNull(this)
            assertEquals("${project.projectDir}/CHANGELOG.md", getInputFile().path)
        }

        (project.tasks.findByName("patchChangelog") as PatchChangelogTask).apply {
            assertNotNull(this)
        }
    }
}
