package org.jetbrains.changelog

import kotlin.test.Test
import kotlin.test.assertEquals

class ExtensionsTest {
    @Test
    fun closureTest() {
        val c = closure { "response" }

        assertEquals("response", c.call())
    }

    @Test
    fun markdownToHTMLTest() {
        val content = """
            # Foo
            ## Bar
            - buz
            - [biz](https://jetbrains.com)
        """.trimIndent()

        assertEquals("""
            <h1>Foo</h1>
            <h2>Bar</h2>
            <ul><li>buz</li><li><a href="https://jetbrains.com">biz</a></li></ul>
        """.trimIndent(), markdownToHTML(content))
    }
}
