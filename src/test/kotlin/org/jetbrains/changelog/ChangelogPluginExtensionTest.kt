// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

import org.jetbrains.changelog.exceptions.MissingFileException
import org.jetbrains.changelog.exceptions.MissingVersionException
import kotlin.test.*

class ChangelogPluginExtensionTest : BaseTest() {

    @BeforeTest
    fun localSetUp() {
        changelog = //language=markdown
            """
            # Changelog
            Project description.
            Multiline description:
            - item 1
            - item 2
            
            ## [Unreleased]
            Not yet released version.
            
            ### Added
            - Foo
            
            ## [1.0.0]
            First release.
            
            ### Removed
            - Bar
            """.trimIndent()
    }

    @Test
    fun `throws MissingFileException when changelog file does not exist`() {
        changelogFile.delete()
        assertFailsWith<MissingFileException> {
            extension.get(version)
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
        extension.get(version).apply {
            assertEquals(project.version, version)

            //language=markdown
            assertEquals(
                """
                ## [1.0.0]
                First release.
                
                ### Removed
                - Bar
                
                """.trimIndent(),
                toText()
            )

            //language=markdown
            assertEquals(
                """
                ## [1.0.0]
                First release.
                
                ### Removed
                - Bar
                
                """.trimIndent(),
                toString()
            )

            //language=markdown
            assertEquals(
                """
                <h2>[1.0.0]</h2>
                <p>First release.</p>
                
                <h3>Removed</h3>
                <ul><li>Bar</li></ul>
                
                """.trimIndent(),
                toHTML()
            )
        }
    }

    @Test
    fun `parses changelog with custom format`() {
        changelog = changelog.replace("""\[([^]]+)]""".toRegex(), "[[$1]]")
        extension.unreleasedTerm.set("[[Unreleased]]")
        extension.get(version).apply {
            assertEquals("1.0.0", version)
        }
    }

    @Test
    fun `getUnreleased() returns Unreleased section`() {
        extension.getUnreleased().apply {
            assertEquals("[Unreleased]", version)
            //language=markdown
            assertEquals(
                """
                ## [Unreleased]
                Not yet released version.
                
                ### Added
                - Foo
                
                """.trimIndent(),
                toText()
            )
        }
    }

    @Test
    fun `getUnreleased() returns Upcoming section if unreleasedTerm is customised`() {
        changelog = changelog.replace("Unreleased", "Upcoming")
        extension.unreleasedTerm.set("[Upcoming]")
        extension.getUnreleased().withSummary(true).apply {
            assertEquals("[Upcoming]", version)
            //language=markdown
            assertEquals(
                """
                ## [Upcoming]
                Not yet released version.
                
                ### Added
                - Foo
                
                """.trimIndent(),
                toText()
            )
        }
    }

    @Test
    @Suppress("LongMethod", "MaxLineLength")
    fun `parses changelog into structured sections`() {
        changelog = //language=markdown
            """
            # Changelog
            My project description.
            
            ## [1.0.0]
            First release.
            
            But a great one.
            
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
            """.trimIndent()

        extension.get(version).apply {
            assertEquals(this@ChangelogPluginExtensionTest.version, version)
            assertEquals("## [1.0.0]", header)
            //language=markdown
            assertEquals(
                """
                First release.
                
                But a great one.
                
                """.trimIndent(),
                summary,
            )
            withHeader(true).withSummary(true).getSections().apply {
                assertEquals(3, size)
                assertTrue(containsKey("Added"))
                assertEquals(6, get("Added")?.size)
                assertTrue(containsKey("Fixed"))
                assertEquals(2, get("Fixed")?.size)
                assertTrue(containsKey("Removed"))
                assertEquals(1, get("Removed")?.size)
            }
            //language=markdown
            assertEquals(
                """
                ## [1.0.0]
                First release.
                
                But a great one.
                
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
                
                """.trimIndent(),
                toText()
            )
            //language=html
            assertEquals(
                """
                <h2>[1.0.0]</h2>
                <p>First release.</p>
                
                <p>But a great one.</p>
                
                <h3>Added</h3>
                <ul><li>Foo <em>FOO</em> foo</li><li>Bar <strong>BAR</strong> bar</li><li>Test <a href="https://www.example.org">link</a> test</li><li>Code <code>block</code> code</li><li>Bravo</li><li>Alpha</li></ul>
                
                <h3>Fixed</h3>
                <ul><li>Hello</li><li>World</li></ul>
                
                <h3>Removed</h3>
                <ul><li>Hola</li></ul>
                
                """.trimIndent(),
                toHTML()
            )
            //language=markdown
            assertEquals(
                """
                [1.0.0]
                First release.
                
                But a great one.
                
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
                
                """.trimIndent(),
                toPlainText()
            )
        }
    }

    @Test
    fun `filters out entries from the change notes for the given version`() {
        changelog = //language=markdown
            """
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
            """.trimIndent()

        extension.get(version).apply {
            assertEquals(this@ChangelogPluginExtensionTest.version, version)
            assertEquals("## [1.0.0]", header)
            withFilter {
                !it.endsWith('x')
            }.apply {

                with (getSections()) {
                    assertEquals(2, size)
                    assertTrue(containsKey("Added"))
                    assertEquals(3, get("Added")?.size)
                    assertTrue(containsKey("Fixed"))
                    assertEquals(1, get("Fixed")?.size)
                    assertFalse(containsKey("Removed"))
                }

                //language=markdown
                assertEquals(
                    """
                    ## [1.0.0]
                    
                    ### Added
                    - Foo
                    - Buz
                    - Alpha
                    
                    ### Fixed
                    - World
                    
                    """.trimIndent(),
                    toText()
                )

                //language=html
                assertEquals(
                    """
                    <h2>[1.0.0]</h2>
                    
                    <h3>Added</h3>
                    <ul><li>Foo</li><li>Buz</li><li>Alpha</li></ul>
                    
                    <h3>Fixed</h3>
                    <ul><li>World</li></ul>
                    
                    """.trimIndent(),
                    toHTML()
                )
            }
        }
    }

    @Test
    fun `returns latest change note`() {
        extension.getLatest().apply {
            assertEquals("[Unreleased]", version)
            assertEquals("## [Unreleased]", header)
            //language=markdown
            assertEquals(
                """
                Not yet released version.
                
                """.trimIndent(),
                summary,
            )
        }
    }

    @Test
    fun `checks if the given version exists in the changelog`() {
        assertTrue(extension.has("[Unreleased]"))
        assertTrue(extension.has("1.0.0"))
        assertFalse(extension.has("2.0.0"))
    }

    @Test
    fun `parses header with custom format containing version and date`() {
        changelog = //language=markdown
            """
            # Changelog
            ## NEW VERSION
            - Compatible with IDEA 2020.2 EAPs
            
            ## Version 1.0.1119-eap (29 May 2020)
            - Compatible with IDEA 2020.2 EAPs
            """.trimIndent()

        extension.unreleasedTerm.set("NEW VERSION")
        extension.get("1.0.1119-eap").apply {
            assertEquals("1.0.1119-eap", version)
        }
    }

    @Test
    fun `returns change notes without group sections if not present`() {
        changelog = //language=markdown
            """
            # Changelog
            ## [1.0.0]
            - Foo
            """.trimIndent()

        extension.get("1.0.0").apply {
            assertEquals("1.0.0", version)

            withHeader(true).getSections().apply {
                assertEquals(1, size)
                assertTrue(containsKey(""))
                assertEquals(1, get("")?.size)
            }
            //language=markdown
            assertEquals(
                """
                ## [1.0.0]
                - Foo
                
                """.trimIndent(),
                toText()
            )
            //language=html
            assertEquals(
                """
                <h2>[1.0.0]</h2>
                <ul><li>Foo</li></ul>
                
                """.trimIndent(),
                toHTML()
            )
        }
    }

    @Test
    fun `splits change notes into a list by the given itemPrefix`() {
        changelog = //language=markdown
            """
            # Changelog
            ## [1.0.0]
            - Foo - bar
            * Foo2
            - Bar
            """.trimIndent()

        extension.get("1.0.0").apply {
            assertEquals("1.0.0", version)
            assertEquals(1, getSections().keys.size)
            getSections().values.first().apply {
                assertEquals(2, size)
                //language=markdown
                assertEquals(
                    """
                    - Foo - bar
                    * Foo2
                    """.trimIndent(),
                    first()
                )
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
            assertEquals("## [Unreleased]", values.first().header)
            assertEquals("## [1.0.0]", values.last().header)
            //language=markdown
            assertEquals(
                """
                ## [Unreleased]
                Not yet released version.
                
                ### Added
                - Foo
                
                """.trimIndent(),
                values.first().toText()
            )
            //language=markdown
            assertEquals(
                """
                ## [Unreleased]
                Not yet released version.
                
                ### Added
                - Foo
                
                """.trimIndent(),
                values.first().toText()
            )
             //language=markdown
            assertEquals(
                """
                ## [1.0.0]
                First release.
                
                ### Removed
                - Bar
                
                """.trimIndent(),
                values.last().toText()
            )
             //language=markdown
            assertEquals(
                """
                ## [1.0.0]
                First release.
                
                ### Removed
                - Bar
                
                """.trimIndent(),
                values.last().toText()
            )
        }
    }

    @Test
    fun `returns Changelog items for change note without category`() {
        extension.itemPrefix.set("*")
        extension.unreleasedTerm.set("Unreleased")
        changelog = //language=markdown
            """
            # My Changelog
            
            ## Unreleased
            
            * Foo
            
            """.trimIndent()

        assertNotNull(extension.getLatest())
        //language=html
        assertEquals(
            """
            <h2>Unreleased</h2>
            <ul><li>Foo</li></ul>
            
            """.trimIndent(),
            extension.getLatest().toHTML()
        )
    }

    @Test
    fun `allows to customize the header parser regex to match version in different format than semver`() {
        changelog = //language=markdown
            """
            # My Changelog
            
            ## 2020.1
            
            * Foo
            """.trimIndent()

        extension.headerParserRegex.set("""\d+\.\d+""".toRegex())
        assertNotNull(extension.get("2020.1"))

        extension.headerParserRegex.set("\\d+\\.\\d+")
        assertNotNull(extension.get("2020.1"))

        extension.headerParserRegex.set("""\d+\.\d+""".toPattern())
        assertNotNull(extension.get("2020.1"))

        assertFailsWith<IllegalArgumentException> {
            extension.headerParserRegex.set(123)
            assertNotNull(extension.get("2020.1"))
        }
    }

    @Test
    fun `return null for non-existent version`() {
        changelog = //language=markdown
            """
            # My Changelog
            
            ## 1.0.0
            
            * Foo
            """.trimIndent()

        assertNull(extension.getOrNull("2.0.0"))
    }

    @Test
    fun `return change notes for version with custom headerParserRegex`() {
        changelog = //language=markdown
            """
            # My Changelog
            
            ## [v1.0.0]
            
            * Foo
            """.trimIndent()

        extension.headerParserRegex.set("\\[?v(\\d(?:\\.\\d+)+)]?.*".toRegex())

        assertNotNull(extension.getOrNull("1.0.0"))
        assertNull(extension.getOrNull("v1.0.0"))
    }

    @Test
    fun `return change notes without summary`() {
        changelog = //language=markdown
            """
            # My Changelog
            
            ## [1.0.0]
            First release.
            
            * Foo
            
            """.trimIndent()

        extension.get("1.0.0").apply {
            assertEquals("1.0.0", version)
            //language=markdown
            assertEquals(
                """
                ## [1.0.0]
                First release.
                
                * Foo
                
                """.trimIndent(),
                toText()
            )
        }
    }

    @Test
    fun `returns changelog description`() {
        //language=markdown
        assertEquals(
            """
            Project description.
            Multiline description:
            
            - item 1
            - item 2
            
            """.trimIndent(),
            extension.instance.introductionValue
        )
    }

    @Test
    fun `applies new changelog introduction`() {
        extension.introduction.set("New introduction")

        assertEquals("New introduction", extension.instance.introductionValue)
    }
}
