// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

import org.gradle.api.Project
import org.gradle.api.internal.provider.AbstractProperty
import org.jetbrains.changelog.tasks.GetChangelogTask
import org.jetbrains.changelog.tasks.InitializeChangelogTask
import org.jetbrains.changelog.tasks.PatchChangelogTask
import java.io.File
import kotlin.test.*
import org.gradle.kotlin.dsl.assign

class ChangelogPluginTest : BaseTest() {

    @Test
    fun `default properties values`() {
        assertNotNull(extension)
        assertTrue(extension.keepUnreleasedSection.get())
        assertEquals(project.file("CHANGELOG.md").canonicalPath, extension.path.get())
        assertEquals("Unreleased", extension.unreleasedTerm.get())
    }

    @Test
    fun `throws VersionNotSpecifiedException when changelog extension has no version provided`() {
        assertFailsWith<AbstractProperty.PropertyQueryException> {
            project.version = Project.DEFAULT_VERSION
            extension.version.get()
        }

        extension.version = "1.0.0"
        assertEquals("1.0.0", extension.version.get())
    }

    @Test
    fun `tasks availability`() {
        (project.tasks.findByName("initializeChangelog") as InitializeChangelogTask).apply {
            assertNotNull(this)
        }

        (project.tasks.findByName("getChangelog") as GetChangelogTask).apply {
            assertNotNull(this)
            assertEquals(File("${project.projectDir}/CHANGELOG.md").path, inputFile.get().asFile.canonicalPath)
        }

        (project.tasks.findByName("patchChangelog") as PatchChangelogTask).apply {
            assertNotNull(this)
        }
    }
}
