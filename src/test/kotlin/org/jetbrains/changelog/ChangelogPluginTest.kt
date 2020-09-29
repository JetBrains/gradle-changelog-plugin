package org.jetbrains.changelog

import org.jetbrains.changelog.tasks.GetChangelogTask
import org.jetbrains.changelog.tasks.InitializeChangelogTask
import org.jetbrains.changelog.tasks.PatchChangelogTask
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChangelogPluginTest : BaseTest() {

    @Test
    fun `default properties values`() {
        assertNotNull(extension)
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
