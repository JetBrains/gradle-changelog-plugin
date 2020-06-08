package org.jetbrains.changelog

import org.jetbrains.changelog.exceptions.MissingFileException
import org.jetbrains.changelog.exceptions.MissingVersionException
import java.text.ParseException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChangelogPluginExtensionTest: BaseTest() {

    @Test
    fun `Throws MissingFileException when changelog file does not exist`() {
        assertFailsWith<MissingFileException> {
            extension.get()
        }
    }

    @Test
    fun `Throws MissingVersionException when project has no version specified`() {
        version = ""
        changelog = """
            # Changelog
        """

        assertFailsWith<MissingVersionException> {
            extension.get()
        }
    }

    @Test
    fun `Throws MissingVersionException if requested unavailable version`() {
        version = "1.0.0"
        changelog = """
            # Changelog
        """

        assertFailsWith<MissingVersionException> {
            extension.get("2.0.0")
        }
    }

    @Test
    fun `Returns a change notes for the v1_0_0`() {
        version = "1.0.0"
        changelog = """
            # Changelog
            
            ## [1.0.0]
            ...
        """

        extension.get().apply {
            assertEquals(project.version, version)

            assertEquals("""
            ## [1.0.0]
            ...
            """.trimIndent(), toText())

            assertEquals("""
                ## [1.0.0]
                ...
            """.trimIndent(), toString())

            // TODO return HTML without <body>
            assertEquals("""
                <body><h2>[1.0.0]</h2><p>...</p></body>
            """.trimIndent(), toHTML())
        }
    }

    @Test
    fun `Parse Changelog with custom format`() {
        version = "1.0.0"
        changelog = """
            # Changelog
            
            ## [[1.0.0]]
        """.trimIndent()

        extension.format = "[[{0}]]"
        extension.get().apply {
            assertEquals("1.0.0", version)
        }
    }

    @Test
    fun `Throws ParseException when Changelog items have unrecognizable header format`() {
        version = "1.0.0"
        changelog = """
            # Changelog
            
            ## ~1.0.0~
            ...
        """

        assertFailsWith<ParseException> {
            extension.get()
        }
    }

    @Test
    fun `headerFormat returns MessageFormat prefixed with Markdown second level header`() {
        assertEquals("## [{0}]", extension.headerFormat().toPattern())
        assertEquals("## ${extension.format}", extension.headerFormat().toPattern())
    }

    @Test
    fun `getUnreleased returns Unreleased section`() {
        version = "1.0.0"
        changelog = """
            # Changelog
            
            ## [Unreleased]
            foo
            
            ## [1.0.0]
            bar
        """

        extension.getUnreleased().apply {
            assertEquals("Unreleased", version)
            assertEquals("""
                ## [Unreleased]
                foo
            """.trimIndent(), toText())
        }
    }

    @Test
    fun `getUnreleased returns Upcoming section if unreleasedTerm is customised`() {
        version = "1.0.0"
        changelog = """
            # Changelog
            
            ## [Upcoming]
            foo
            
            ## [1.0.0]
            bar
        """

        extension.unreleasedTerm = "Upcoming"
        extension.getUnreleased().apply {
            assertEquals("Upcoming", version)
            assertEquals("""
                ## [Upcoming]
                foo
            """.trimIndent(), toText())
        }
    }
}
