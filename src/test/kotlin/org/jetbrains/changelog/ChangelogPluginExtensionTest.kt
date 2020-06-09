package org.jetbrains.changelog

import org.jetbrains.changelog.exceptions.MissingFileException
import org.jetbrains.changelog.exceptions.MissingVersionException
import java.io.File
import java.text.ParseException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChangelogPluginExtensionTest : BaseTest() {

    @BeforeTest
    fun localSetUp() {
        version = "1.0.0"
        changelog = """
            # Changelog
            
            ## [Unreleased]
            ### Added
            - Foo
            
            ## [1.0.0]
            ### Removed
            - Bar
        """
    }

    @Test
    fun `throws MissingFileException when changelog file does not exist`() {
        File(extension.path).delete()
        assertFailsWith<MissingFileException> {
            extension.get()
        }
    }

    @Test
    fun `throws MissingVersionException when project has no version specified`() {
        version = ""

        assertFailsWith<MissingVersionException> {
            extension.get()
        }
    }

    @Test
    fun `throws MissingVersionException if requested version is not available`() {
        assertFailsWith<MissingVersionException> {
            extension.get("2.0.0")
        }
    }

    @Test
    fun `returns change notes for the v1_0_0 version`() {
        extension.get().apply {
            assertEquals(project.version, version)

            assertEquals("""
                ### Removed
                - Bar
            """.trimIndent(), toText())

            assertEquals("""
                ### Removed
                - Bar
            """.trimIndent(), toString())

            // TODO return HTML without <body>
            assertEquals("""
                <h3>Removed</h3>
                <ul><li>Bar</li></ul>
            """.trimIndent(), toHTML())
        }
    }

    @Test
    fun `parses changelog with custom format`() {
        changelog = changelog.replace("""\[([^]]+)\]""".toRegex(), "[[$1]]")
        extension.format = "[[{0}]]"
        extension.get().apply {
            assertEquals("1.0.0", version)
        }
    }

    @Test
    fun `throws ParseException when changelog item has unrecognizable header format`() {
        changelog = """
            # Changelog
            
            ## ~1.0.0~
            ### Added
            - Foo
        """

        assertFailsWith<ParseException> {
            extension.get()
        }
    }

    @Test
    fun `headerFormat() returns MessageFormat prefixed with Markdown second level header`() {
        assertEquals("## [{0}]", extension.headerFormat().toPattern())
        assertEquals("## ${extension.format}", extension.headerFormat().toPattern())
    }

    @Test
    fun `getUnreleased() returns Unreleased section`() {
        extension.getUnreleased().withHeader(true).apply {
            assertEquals("Unreleased", version)
            assertEquals("""
                ## [Unreleased]
                ### Added
                - Foo
            """.trimIndent(), toText())
        }
    }

    @Test
    fun `getUnreleased() returns Upcoming section if unreleasedTerm is customised`() {
        changelog = changelog.replace("Unreleased", "Upcoming")
        extension.unreleasedTerm = "Upcoming"
        extension.getUnreleased().withHeader(true).apply {
            assertEquals("Upcoming", version)
            assertEquals("""
                ## [Upcoming]
                ### Added
                - Foo
            """.trimIndent(), toText())
        }
    }

    @Test
    fun `parses changelog into structured sections`() {
        changelog = """
            # Changelog
            
            ## [1.0.0]
            ### Added
            - Foo
            - Bar
            - Bravo
            - Alpha
            
            ### Fixed
            - Hello
            - World
            
            ### Removed
            - Hola
        """

        extension.get().apply {
            assertEquals(this@ChangelogPluginExtensionTest.version, version)
            assertEquals("## [1.0.0]", getHeader())
            withHeader(true).getSections().apply {
                assertEquals(3, size)
                assertTrue(containsKey("Added"))
                assertEquals(4, get("Added")?.size)
                assertTrue(containsKey("Fixed"))
                assertEquals(2, get("Fixed")?.size)
                assertTrue(containsKey("Removed"))
                assertEquals(1, get("Removed")?.size)
            }
            assertEquals("""
                ## [1.0.0]
                ### Added
                - Foo
                - Bar
                - Bravo
                - Alpha

                ### Fixed
                - Hello
                - World

                ### Removed
                - Hola
            """.trimIndent(), toText())
            assertEquals("""
                <h2>[1.0.0]</h2>
                <h3>Added</h3>
                <ul><li>Foo</li><li>Bar</li><li>Bravo</li><li>Alpha</li></ul>
                
                <h3>Fixed</h3>
                <ul><li>Hello</li><li>World</li></ul>
                
                <h3>Removed</h3>
                <ul><li>Hola</li></ul>
            """.trimIndent(), toHTML())
        }
    }

    @Test
    fun `filters out entries from the change notes for the given version`() {
        changelog = """
            # Changelog
            
            ## [1.0.0]
            ### Added
            - Foo
            - Bar x
            - Buz
            - Bravo x
            - Alpha
            
            ### Fixed
            - Hello x
            - World
            
            ### Removed
            - Hola x
        """

        extension.get().apply {
            assertEquals(this@ChangelogPluginExtensionTest.version, version)
            assertEquals("## [1.0.0]", getHeader())
            withFilter {
                !it.endsWith('x')
            }.getSections().apply {
                assertEquals(2, size)
                assertTrue(containsKey("Added"))
                assertEquals(3, get("Added")?.size)
                assertTrue(containsKey("Fixed"))
                assertEquals(1, get("Fixed")?.size)
                assertFalse(containsKey("Removed"))

                assertEquals("""
                    ### Added
                    - Foo
                    - Buz
                    - Alpha

                    ### Fixed
                    - World
                """.trimIndent(), toText())
                assertEquals("""
                    <h3>Added</h3>
                    <ul><li>Foo</li><li>Buz</li><li>Alpha</li></ul>

                    <h3>Fixed</h3>
                    <ul><li>World</li></ul>
                """.trimIndent(), toHTML())
            }
        }
    }

    @Test
    fun `returns latest change note`() {
        extension.getLatest().apply {
            assertEquals("Unreleased", version)
            assertEquals("## [Unreleased]", getHeader())
        }
    }

    @Test
    fun `checks if the given version exists in the changelog`() {
        assertTrue(extension.hasVersion("Unreleased"))
        assertTrue(extension.hasVersion("1.0.0"))
        assertFalse(extension.hasVersion("2.0.0"))
    }
}
