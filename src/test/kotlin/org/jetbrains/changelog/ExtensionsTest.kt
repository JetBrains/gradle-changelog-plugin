// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

import java.text.SimpleDateFormat
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals

class ExtensionsTest {

    @Test
    fun dateTest() {
        assertEquals(SimpleDateFormat("yyyy-MM-dd").format(Date()), date())

        val pattern = "ddMMyyyy"
        assertEquals(SimpleDateFormat(pattern).format(Date()), date(pattern))
    }

    @Test
    fun markdownToHTMLTest() {
        val content =
            """
            # Foo
            ## Bar
            - buz
            - [biz](https://jetbrains.com)
            """.trimIndent()

        assertEquals(
            """
            <h1>Foo</h1>
            <h2>Bar</h2>
            <ul><li>buz</li><li><a href="https://jetbrains.com">biz</a></li></ul>
            """.trimIndent(),
            markdownToHTML(content)
        )
    }

    @Test
    fun markdownToPlainTextTest() {
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
            markdownToPlainText(content)
        )
    }
}
