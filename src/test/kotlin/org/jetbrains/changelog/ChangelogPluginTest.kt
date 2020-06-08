package org.jetbrains.changelog

import org.jetbrains.changelog.tasks.GetChangelogTask
import org.jetbrains.changelog.tasks.PatchChangelogTask
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChangelogPluginTest : BaseTest() {

    @Test
    fun `default properties values`() {
        assertNotNull(extension)
        assertEquals("[{0}]", extension.format)
        assertEquals("## [{0}]", extension.headerFormat().toPattern())
        assertTrue(extension.keepUnreleasedSection)
        assertEquals("${project.projectDir}/CHANGELOG.md", extension.path)
        assertEquals(project.version, extension.version)
        assertEquals("Unreleased", extension.unreleasedTerm)
    }

    @Test
    fun `tasks availability`() {
        val getChangelogTask = project.tasks.findByName("getChangelog") as GetChangelogTask
        assertNotNull(getChangelogTask)
        assertEquals("${project.projectDir}/CHANGELOG.md", getChangelogTask.getInputFile().path)

        val patchChangelogTask = project.tasks.findByName("patchChangelog") as PatchChangelogTask
        assertNotNull(patchChangelogTask)
    }
}
