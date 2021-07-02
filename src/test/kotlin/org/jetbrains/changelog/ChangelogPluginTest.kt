package org.jetbrains.changelog

import org.gradle.api.Project
import org.gradle.api.internal.provider.AbstractProperty
import org.jetbrains.changelog.tasks.GetChangelogTask
import org.jetbrains.changelog.tasks.InitializeChangelogTask
import org.jetbrains.changelog.tasks.PatchChangelogTask
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChangelogPluginTest : BaseTest() {

    @Test
    fun `default properties values`() {
        assertNotNull(extension)
        assertTrue(extension.keepUnreleasedSection.get())
        assertEquals("${project.projectDir}/CHANGELOG.md", extension.path.get())
        assertEquals("[Unreleased]", extension.unreleasedTerm.get())
    }

    @Test
    fun `throws VersionNotSpecifiedException when changelog extension has no version provided`() {
        assertFailsWith<AbstractProperty.PropertyQueryException> {
            project.version = Project.DEFAULT_VERSION
            extension.version.get()
        }

        extension.version.set("1.0.0")
        assertEquals("1.0.0", extension.version.get())
    }

    @Test
    fun `tasks availability`() {
        (project.tasks.findByName("initializeChangelog") as InitializeChangelogTask).apply {
            assertNotNull(this)
        }

        (project.tasks.findByName("getChangelog") as GetChangelogTask).apply {
            assertNotNull(this)
            assertEquals(File("${project.projectDir}/CHANGELOG.md").path, getInputFile().path)
        }

        (project.tasks.findByName("patchChangelog") as PatchChangelogTask).apply {
            assertNotNull(this)
        }
    }
}
