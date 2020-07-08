package org.jetbrains.changelog

import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.jetbrains.changelog.exceptions.HeaderParseException
import org.jetbrains.changelog.exceptions.MissingFileException
import org.jetbrains.changelog.exceptions.MissingVersionException

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
        changelog = changelog.replace("""\[([^]]+)]""".toRegex(), "[[$1]]")
        extension.headerFormat = "[[{0}]]"
        extension.get().apply {
            assertEquals("1.0.0", version)
        }
    }

    @Test
    fun `throws ParseException when changelog item has unrecognizable header format`() {
        changelog = """
            # [Changelog]
            
            ## ~1.0.0~
            ### Added
            - Foo
        """

        assertFailsWith<HeaderParseException> {
            extension.get()
        }
    }

    @Test
    fun `headerFormat() returns MessageFormat prefixed with Markdown second level header`() {
        assertEquals("[{0}]", extension.headerMessageFormat().toPattern())
        assertEquals(extension.headerFormat, extension.headerMessageFormat().toPattern())
    }

    @Test
    fun `getUnreleased() returns Unreleased section`() {
        extension.getUnreleased().withHeader(true).apply {
            assertEquals("[Unreleased]", version)
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
            - Foo *FOO* foo
            - Bar **BAR** bar
            - Test [link](https://www.example.org) test
            - Code `block` code
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
                assertEquals(6, get("Added")?.size)
                assertTrue(containsKey("Fixed"))
                assertEquals(2, get("Fixed")?.size)
                assertTrue(containsKey("Removed"))
                assertEquals(1, get("Removed")?.size)
            }
            assertEquals("""
                ## [1.0.0]
                ### Added
                - Foo *FOO* foo
                - Bar **BAR** bar
                - Test [link](https://www.example.org) test
                - Code `block` code
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
                <ul><li>Foo <em>FOO</em> foo</li><li>Bar <strong>BAR</strong> bar</li><li>Test <a href="https://www.example.org">link</a> test</li><li>Code <code>block</code> code</li><li>Bravo</li><li>Alpha</li></ul>
                
                <h3>Fixed</h3>
                <ul><li>Hello</li><li>World</li></ul>
                
                <h3>Removed</h3>
                <ul><li>Hola</li></ul>
            """.trimIndent(), toHTML())
            assertEquals("""
                [1.0.0]
                Added
                - Foo FOO foo
                - Bar BAR bar
                - Test link test
                - Code block code
                - Bravo
                - Alpha
                
                Fixed
                - Hello
                - World
                
                Removed
                - Hola
            """.trimIndent(), toPlainText())
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
            assertEquals("[Unreleased]", version)
            assertEquals("## [Unreleased]", getHeader())
        }
    }

    @Test
    fun `checks if the given version exists in the changelog`() {
        assertTrue(extension.hasVersion("[Unreleased]"))
        assertTrue(extension.hasVersion("1.0.0"))
        assertFalse(extension.hasVersion("2.0.0"))
    }

    @Test
    fun `parses header with custom format containing version and date`() {
        changelog = """
            # Changelog
            ## NEW VERSION
            - Compatible with IDEA 2020.2 EAPs
            
            ## Version 1.0.1119-eap (29 May 2020)
            - Compatible with IDEA 2020.2 EAPs
        """

        extension.headerFormat = "Version {0} ({1})"
        extension.unreleasedTerm = "NEW VERSION"
        extension.get("1.0.1119-eap").apply {
            assertEquals("1.0.1119-eap", version)

            val headerVariables = extension.headerMessageFormat().parse(getHeader().removePrefix("## "))
            assertEquals(2, headerVariables.size)
            assertEquals("1.0.1119-eap", headerVariables[0])
            assertEquals("29 May 2020", headerVariables[1])
        }
    }

    @Test
    fun `returns change notes without group sections if not present`() {
        changelog = """
            # Changelog
            ## [1.0.0]
            - Foo
        """

        extension.get("1.0.0").apply {
            assertEquals("1.0.0", version)

            withHeader(true).getSections().apply {
                assertEquals(1, size)
                assertTrue(containsKey(""))
                assertEquals(1, get("")?.size)
            }
            assertEquals("""
                ## [1.0.0]
                - Foo
            """.trimIndent(), toText())
            assertEquals("""
                <h2>[1.0.0]</h2>
                <ul><li>Foo</li></ul>
            """.trimIndent(), toHTML())
        }
    }

    @Test
    fun `splits change notes into a list by the given itemPrefix`() {
        changelog = """
            # Changelog
            ## [1.0.0]
            - Foo - bar
            * Foo2
            - Bar
        """

        extension.get("1.0.0").apply {
            assertEquals("1.0.0", version)
            assertEquals(1, getSections().keys.size)
            getSections().values.first().apply {
                assertEquals(2, size)
                assertEquals("""
                    - Foo - bar
                    * Foo2
                """.trimIndent(), first())
                assertEquals("- Bar", last())
            }
        }
    }

    @Test
    fun `returns all Changelog items`() {
        extension.getAll().apply {
            assertNotNull(this)
            assertEquals(2, keys.size)
            assertEquals("[Unreleased]", keys.first())
            assertEquals("1.0.0", keys.last())
            assertEquals("## [Unreleased]", values.first().getHeader())
            assertEquals("## [1.0.0]", values.last().getHeader())
            assertEquals("""
                ### Added
                - Foo
            """.trimIndent(), values.first().toText())
            assertEquals("""
                ## [Unreleased]
                ### Added
                - Foo
            """.trimIndent(), values.first().withHeader(true).toText())
            assertEquals("""
                ### Removed
                - Bar
            """.trimIndent(), values.last().toText())
            assertEquals("""
                ## [1.0.0]
                ### Removed
                - Bar
            """.trimIndent(), values.last().withHeader(true).toText())
        }
    }
}
