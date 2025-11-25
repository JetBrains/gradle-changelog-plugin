// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

import org.gradle.kotlin.dsl.assign
import org.jetbrains.changelog.exceptions.MissingVersionException
import kotlin.test.*

class ChangelogPluginExtensionTest : BaseTest() {

    @BeforeTest
    fun localSetUp() {
        changelog =
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
            
            [Unreleased]: https://blog.jetbrains.com
            [1.0.0]: https://jetbrains.com
            """.trimIndent()

        buildFile =
            """
            plugins {
                id 'org.jetbrains.changelog'
            }
            changelog {
                repositoryUrl = "https://github.com/JetBrains/gradle-changelog-plugin"
            }
            """.trimIndent()
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

            assertMarkdown(
                """
                ## [1.0.0]
                
                First release.
                
                ### Removed
                
                - Bar
                
                [1.0.0]: https://jetbrains.com
                """.trimIndent(),
                extension.renderItem(this),
            )

            assertText(
                """
                1.0.0
                
                First release.
                
                Removed
                
                - Bar
                """.trimIndent(),
                extension.renderItem(this, Changelog.OutputType.PLAIN_TEXT),
            )

            assertHTML(
                """
                <h2><a href="https://jetbrains.com">1.0.0</a></h2>
                
                <p>First release.</p>
                
                <h3>Removed</h3>
                
                <ul><li>Bar</li></ul>
                
                """.trimIndent(),
                extension.renderItem(this, Changelog.OutputType.HTML),
            )
        }
    }

    @Test
    fun `parses changelog with custom format`() {
        changelog = changelog.replace("""\[([^]]+)]""".toRegex(), "[[$1]]")
        extension.unreleasedTerm = "[[Unreleased]]"
        extension.get(version).apply {
            assertEquals("1.0.0", version)
        }
    }

    @Test
    fun `getUnreleased() returns Unreleased section`() {
        extension.getUnreleased().apply {
            assertEquals("Unreleased", version)
            assertEquals("Unreleased", header)
            assertMarkdown(
                """
                ## [Unreleased]
                
                Not yet released version.
                
                ### Added
                
                - Foo
                
                [Unreleased]: https://blog.jetbrains.com
                """.trimIndent(),
                extension.renderItem(this),
            )
        }
    }

    @Test
    fun `getUnreleased() returns Upcoming section if unreleasedTerm is customised`() {
        changelog = changelog.replace("Unreleased", "Upcoming")
        extension.unreleasedTerm = "Upcoming"
        extension.getUnreleased().apply {
            assertEquals("Upcoming", version)
            assertMarkdown(
                """
                ## [Upcoming]
                
                Not yet released version.
                
                ### Added
                
                - Foo
                
                [Upcoming]: https://blog.jetbrains.com
                """.trimIndent(),
                extension.renderItem(this),
            )
        }
    }

    @Test
    @Suppress("LongMethod", "MaxLineLength")
    fun `parses changelog into structured sections`() {
        changelog =
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
            assertEquals("1.0.0", header)
            assertMarkdown(
                """
                First release.
                
                But a great one.
                
                """.trimIndent(),
                summary,
            )
            withHeader(true).withSummary(true).sections.apply {
                assertEquals(3, size)
                assertTrue(containsKey("Added"))
                assertEquals(6, get("Added")?.size)
                assertTrue(containsKey("Fixed"))
                assertEquals(2, get("Fixed")?.size)
                assertTrue(containsKey("Removed"))
                assertEquals(1, get("Removed")?.size)
            }
            assertMarkdown(
                """
                ## 1.0.0
                
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
                extension.renderItem(this),
            )
            assertHTML(
                """
                <h2>1.0.0</h2>
                
                <p>First release.</p>
                
                <p>But a great one.</p>
                
                <h3>Added</h3>
                
                <ul><li>Foo <em>FOO</em> foo</li><li>Bar <strong>BAR</strong> bar</li><li>Test <a href="https://www.example.org">link</a> test</li><li>Code <code>block</code> code</li><li>Bravo</li><li>Alpha</li></ul>
                
                <h3>Fixed</h3>
                
                <ul><li>Hello</li><li>World</li></ul>
                
                <h3>Removed</h3>
                
                <ul><li>Hola</li></ul>
                
                """.trimIndent(),
                extension.renderItem(this, Changelog.OutputType.HTML),
            )
            assertText(
                """
                1.0.0
                
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
                extension.renderItem(this, Changelog.OutputType.PLAIN_TEXT),
            )
        }
    }

    @Test
    fun `filters out entries from the change notes for the given version`() {
        changelog =
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
            assertEquals("1.0.0", header)
            withFilter {
                !it.endsWith('x')
            }.apply {

                with(sections) {
                    assertEquals(2, size)
                    assertTrue(containsKey("Added"))
                    assertEquals(3, get("Added")?.size)
                    assertTrue(containsKey("Fixed"))
                    assertEquals(1, get("Fixed")?.size)
                    assertFalse(containsKey("Removed"))
                }

                assertMarkdown(
                    """
                    ## 1.0.0
                    
                    ### Added
                    
                    - Foo
                    - Buz
                    - Alpha
                    
                    ### Fixed
                    
                    - World
                    
                    """.trimIndent(),
                    extension.renderItem(this),
                )

                assertHTML(
                    """
                    <h2>1.0.0</h2>
                    
                    <h3>Added</h3>
                    
                    <ul><li>Foo</li><li>Buz</li><li>Alpha</li></ul>
                    
                    <h3>Fixed</h3>
                    
                    <ul><li>World</li></ul>
                    
                    """.trimIndent(),
                    extension.renderItem(this, Changelog.OutputType.HTML),
                )
            }
        }
    }

    @Test
    fun `returns latest change note`() {
        extension.getLatest().apply {
            assertEquals("1.0.0", version)
            assertEquals("1.0.0", header)
            assertMarkdown(
                """
                First release.
                
                """.trimIndent(),
                summary,
            )
        }
    }

    @Test
    fun `checks if the given version exists in the changelog`() {
        assertTrue(extension.has("Unreleased"))
        assertTrue(extension.has("1.0.0"))
        assertFalse(extension.has("2.0.0"))
    }

    @Test
    fun `parses header with custom format containing version and date`() {
        changelog =
            """
            # Changelog
            
            ## NEW VERSION
            
            - Compatible with IDEA 2020.2 EAPs
            
            ## Version 1.0.1119-eap (29 May 2020)
            
            - Compatible with IDEA 2020.2 EAPs
            """.trimIndent()

        extension.unreleasedTerm = "NEW VERSION"
        extension.get("1.0.1119-eap").apply {
            assertEquals("1.0.1119-eap", version)
        }
    }

    @Test
    fun `returns change notes without group sections if not present`() {
        changelog =
            """
            # Changelog
            
            ## [1.0.0]
            
            - Foo
            """.trimIndent()

        extension.get("1.0.0").apply {
            assertEquals("1.0.0", version)

            withHeader(true).sections.apply {
                assertEquals(1, size)
                assertTrue(containsKey(""))
                assertEquals(1, get("")?.size)
            }
            assertMarkdown(
                """
                ## 1.0.0
                
                - Foo
                
                """.trimIndent(),
                extension.renderItem(this),
            )
            assertHTML(
                """
                <h2>1.0.0</h2>
                
                <ul><li>Foo</li></ul>
                
                """.trimIndent(),
                extension.renderItem(this, Changelog.OutputType.HTML),
            )
        }
    }

    @Test
    fun `splits change notes into a list by the given itemPrefix`() {
        changelog =
            """
            # Changelog
            
            ## [1.0.0]
            
            - Foo - bar
            * Foo2
            - Bar
            """.trimIndent()

        extension.get("1.0.0").apply {
            assertEquals("1.0.0", version)
            assertEquals(1, sections.keys.size)
            sections.values.first().apply {
                assertEquals(3, size)
                assertMarkdown("Foo - bar", first())
                assertEquals("Bar", last())
            }
        }
    }

    @Test
    fun `returns all Changelog items`() {
        extension.getAll().apply {
            assertNotNull(this)
            assertEquals(2, keys.size)
            assertEquals("Unreleased", keys.first())
            assertEquals("1.0.0", keys.last())
            assertEquals("Unreleased", values.first().header)
            assertEquals("1.0.0", values.last().header)
            assertMarkdown(
                """
                ## [Unreleased]
                
                Not yet released version.
                
                ### Added
                
                - Foo
                
                [Unreleased]: https://blog.jetbrains.com
                
                """.trimIndent(),
                extension.renderItem(values.first()),
            )
            assertMarkdown(
                """
                ## [1.0.0]
                
                First release.
                
                ### Removed
                
                - Bar
                
                [1.0.0]: https://jetbrains.com
                
                """.trimIndent(),
                extension.renderItem(values.last()),
            )
        }
    }

    @Test
    fun `returns Changelog items for change note without category`() {
        extension.itemPrefix = "*"
        extension.unreleasedTerm = "Unreleased"
        changelog =
            """
            # My Changelog
            
            ## Unreleased
            
            * Foo
            
            """.trimIndent()

        assertNotNull(extension.getUnreleased())
        assertHTML(
            """
            <h2>Unreleased</h2>
            
            <ul><li>Foo</li></ul>
            
            """.trimIndent(),
            extension.renderItem(extension.getUnreleased(), Changelog.OutputType.HTML),
        )
    }

    @Test
    fun `allows to customize the header parser regex to match version in different format than semver`() {
        changelog =
            """
            # My Changelog
            
            ## 2020.1
            
            * Foo
            """.trimIndent()

        extension.headerParserRegex = """\d+\.\d+""".toRegex()
        assertNotNull(extension.get("2020.1"))

        extension.headerParserRegex = "\\d+\\.\\d+"
        assertNotNull(extension.get("2020.1"))

        extension.headerParserRegex = """\d+\.\d+""".toPattern()
        assertNotNull(extension.get("2020.1"))

        assertFails {
            extension.headerParserRegex = 123
            assertNotNull(extension.get("2020.1"))
        }
    }

    @Test
    fun `return null for non-existent version`() {
        changelog =
            """
            # My Changelog
            
            ## 1.0.0
            
            * Foo
            """.trimIndent()

        assertNull(extension.getOrNull("2.0.0"))
    }

    @Test
    fun `return change notes for version with custom headerParserRegex`() {
        changelog =
            """
            # My Changelog
            
            ## [v1.0.0]
            
            * Foo
            """.trimIndent()

        extension.headerParserRegex = "\\[?v(\\d(?:\\.\\d+)+)]?.*".toRegex()

        assertNotNull(extension.getOrNull("1.0.0"))
        assertNull(extension.getOrNull("v1.0.0"))
    }

    @Test
    fun `return change notes without summary`() {
        changelog =
            """
            # My Changelog
            
            ## [1.0.0]
            
            First release.
            
            * Foo
            
            """.trimIndent()

        extension.get("1.0.0").apply {
            assertEquals("1.0.0", version)
            assertMarkdown(
                """
                ## 1.0.0
                
                First release.
                
                - Foo
                
                """.trimIndent(),
                extension.renderItem(this),
            )
        }
    }

    @Test
    fun `returns changelog description`() {
        assertMarkdown(
            """
            Project description.
            Multiline description:
            - item 1
            - item 2
            
            """.trimIndent(),
            extension.instance.get().introduction,
        )
    }

    @Test
    fun `applies new changelog introduction`() {
        extension.introduction = "New introduction"

        assertEquals("New introduction", extension.instance.get().introduction)
    }

    @Test
    fun `provide a custom sectionUrlBuilder`() {
        changelog =
            """
            # Changelog
            
            Foo
            
            ## [Unreleased]
            
            Not yet released version.
            
            ### Added
            
            - Foo
            
            ## [0.0.1]
            
            ### Added
            - Bar
            """.trimIndent()

        val customRepositoryUrl = "https://github.com/JetBrains/gradle-changelog-plugin"

        extension.repositoryUrl = customRepositoryUrl
        extension.sectionUrlBuilder =
            ChangelogSectionUrlBuilder { repositoryUrl, currentVersion, previousVersion, isUnreleased ->
                "repositoryUrl = $repositoryUrl | currentVersion = $currentVersion | previousVersion = $previousVersion | isUnreleased = $isUnreleased"
            }

        val items = extension.instance.get().links.values

        assertEquals(
            "repositoryUrl = $customRepositoryUrl | currentVersion = Unreleased | previousVersion = 0.0.1 | isUnreleased = true",
            items.first(),
        )
        assertEquals(
            "repositoryUrl = $customRepositoryUrl | currentVersion = 0.0.1 | previousVersion = null | isUnreleased = false",
            items.last(),
        )
    }

    @Test
    fun `provide a custom sectionUrlBuilder with extra parameters for Bitbucket`() {
        changelog =
            """
            # Changelog
            
            Foo
            
            ## [Unreleased]
            
            Not yet released version.
            
            ### Added
            
            - Foo
            
            ## [0.0.1]
            
            ### Added
            - Bar
            """.trimIndent()

        val customRepositoryUrl = "https://bitbucket.org/myorg/myrepo"

        extension.repositoryUrl = customRepositoryUrl
        extension.sectionUrlBuilder =
            object : ChangelogSectionUrlBuilder {
                override val extraParams = mapOf("branch" to "main")

                override fun build(
                    repositoryUrl: String,
                    currentVersion: String?,
                    previousVersion: String?,
                    isUnreleased: Boolean,
                ): String {
                    val branch = extraParams["branch"] ?: "master"
                    return "$repositoryUrl/branches/compare/$currentVersion..$previousVersion#diff | branch=$branch"
                }
            }

        val items = extension.instance.get().links.values

        assertTrue(items.first().contains("branch=main"))
        assertTrue(items.first().contains(customRepositoryUrl))
    }

    @Test
    fun `returns change notes for the v1_0_0 version of changelog that use CRLF`() {
        //language=Markdown
        changelog =
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
            
            ### Added
            
            - Test [link](https://www.example.org) test
            
            ### Removed
            
            - Bar
            
            [Unreleased]: https://blog.jetbrains.com
            [1.0.0]: https://jetbrains.com
            """.trimIndent().normalizeLineSeparator("\r\n")

        extension.get(version).apply {
            assertEquals(project.version, version)

            //language=Markdown
            assertMarkdown(
                """
                ## [1.0.0]
                
                First release.
                
                ### Added
                
                - Test [link](https://www.example.org) test
                
                ### Removed
                
                - Bar
                
                [1.0.0]: https://jetbrains.com
                """.trimIndent().normalizeLineSeparator("\r\n"),
                extension.renderItem(this),
            )

            //language=HTML
            assertHTML(
                """
                <h2><a href="https://jetbrains.com">1.0.0</a></h2>
                
                <p>First release.</p>
                
                <h3>Added</h3>
                
                <ul><li>Test <a href="https://www.example.org">link</a> test</li></ul>
                
                <h3>Removed</h3>
                
                <ul><li>Bar</li></ul>
                """.trimIndent().normalizeLineSeparator("\r\n"),
                extension.renderItem(this, Changelog.OutputType.HTML),
            )

            assertText(
                """
                1.0.0
                
                First release.
                
                Added
                
                - Test link test
                
                Removed
                
                - Bar
                """.trimIndent().normalizeLineSeparator("\r\n"),
                extension.renderItem(this, Changelog.OutputType.PLAIN_TEXT),
            )
        }
    }

    @Test
    @Suppress("LongMethod", "MaxLineLength")
    fun `render changelog that use CRLF`() {
        //language=Markdown
        changelog =
            """
            # Changelog
            
            My project description.
            
            ## [1.1.0]
            
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
            
            ## [1.0.0]
            
            ### Added
            
            - Foo 1.0.0
            - Bar 1.0.0
            
            ### Removed
            
            - Removed 1.0.0
            
            [1.1.0]: https://jetbrains.com/1.1.0
            [1.0.0]: https://jetbrains.com/1.0.0
            """.trimIndent().normalizeLineSeparator("\r\n")

        extension.get(version).apply {

            //language=Markdown
            assertMarkdown(
                """
                # Changelog
                
                My project description.
                
                ## [1.1.0]
                
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
                
                ## [1.0.0]
                
                ### Added
                
                - Foo 1.0.0
                - Bar 1.0.0
                
                ### Removed
                
                - Removed 1.0.0
                
                [1.1.0]: https://jetbrains.com/1.1.0
                [1.0.0]: https://jetbrains.com/1.0.0
                """.trimIndent().normalizeLineSeparator("\r\n"),
                extension.render(Changelog.OutputType.MARKDOWN),
            )
            //language=HTML
            assertHTML(
                """
                <h1>Changelog</h1>
                
                <p>My project description.</p>
                
                <h2><a href="https://jetbrains.com/1.1.0">1.1.0</a></h2>
                
                <p>First release.</p>
                
                <p>But a great one.</p>
                
                <h3>Added</h3>
                
                <ul><li>Foo <em>FOO</em> foo</li><li>Bar <strong>BAR</strong> bar</li><li>Test <a href="https://www.example.org">link</a> test</li><li>Code <code>block</code> code</li><li>Bravo</li><li>Alpha</li></ul>
                
                <h3>Fixed</h3>
                
                <ul><li>Hello</li><li>World</li></ul>
                
                <h3>Removed</h3>
                
                <ul><li>Hola</li></ul>
                
                <h2><a href="https://jetbrains.com/1.0.0">1.0.0</a></h2>
                
                <h3>Added</h3>
                
                <ul><li>Foo 1.0.0</li><li>Bar 1.0.0</li></ul>
                
                <h3>Removed</h3>
                
                <ul><li>Removed 1.0.0</li></ul>
                """.trimIndent().normalizeLineSeparator("\r\n"),
                extension.render(Changelog.OutputType.HTML),
            )
            assertText(
                """
                Changelog
                
                My project description.
                
                1.1.0
                
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
                
                1.0.0
                
                Added
                
                - Foo 1.0.0
                - Bar 1.0.0
                
                Removed
                
                - Removed 1.0.0
                """.trimIndent().normalizeLineSeparator("\r\n"),
                extension.render(Changelog.OutputType.PLAIN_TEXT),
            )
        }
    }
}
