// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

import org.intellij.lang.annotations.Language
import java.text.SimpleDateFormat
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ExtensionsTest {

    private val lineSeparator = "\n"

    @Test
    fun dateTest() {
        assertEquals(SimpleDateFormat("yyyy-MM-dd").format(Date()), date())

        val pattern = "ddMMyyyy"
        assertEquals(SimpleDateFormat(pattern).format(Date()), date(pattern))
    }

    @Test
    fun markdownToHTMLTest() {
        @Language("Markdown")
        val content =
            """
            # Foo
            ## Bar
            - buz
            - [biz](https://jetbrains.com)
            """.trimIndent()

        @Language("HTML")
        val html =
            """
            <h1>Foo</h1>
            <h2>Bar</h2>
            <ul><li>buz</li><li><a href="https://jetbrains.com">biz</a></li></ul>
            """.trimIndent()

        assertEquals(html, markdownToHTML(content))
    }

    @Test
    fun markdownToPlainTextTest() {
        @Language("Markdown")
        val content =
            """
            # Foo
            ## Bar
            - buz
            - [biz](https://jetbrains.com)
            """.trimIndent()

        assertEquals(
            """
            Foo
            Bar
            - buz
            - biz
            """.trimIndent(),
            markdownToPlainText(content, lineSeparator)
        )
    }

    @Test
    fun `reformat changelog`() {
        assertEquals(
            """
            Pre title content.
            
            # Title
            Summary
            
            ## [Unreleased]
            
            ## [1.0.0]
            - asd
            
            ## [0.1.0]
            
            ### Added
            - Buz
            
            """.trimIndent(),
            """
            Pre title content.
            # Title
            Summary
            ## [Unreleased]
            ## [1.0.0]
            - asd
            ## [0.1.0]
            ### Added
            - Buz
            """.trimIndent().reformat(lineSeparator)
        )

        assertEquals(
            """
            Foo
            
            # My Title
            Introduction
            
            ## Upcoming version
            
            ### Added
            
            ### Removed
            
            """.trimIndent(),
            """
            Foo
            # My Title
            Introduction
            ## Upcoming version
            ### Added
            ### Removed
            """.trimIndent().reformat(lineSeparator)
        )
    }
}
