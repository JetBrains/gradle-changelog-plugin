// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

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

    @Test
    fun `normalize string line separator`() {
        val text = """
            Pre title content.
            
            # Title
            Summary
            
            ## [Unreleased]
            
            ## [1.0.0]
            - asd
            
            ## [0.1.0]
            
            ### Added
            - Buz
            
            """.trimIndent()

        assertEquals(
            text,
            text.replace("\n", "\r\n").normalizeLineSeparator("\n")
        )

        assertEquals(
            text,
            text.replace("\n", "\r").normalizeLineSeparator("\n")
        )

        assertEquals(
            text,
            text.normalizeLineSeparator("\n")
        )

        assertEquals(
            "text\ntext2\ntext3\ntext4",
            "text\ntext2\rtext3\r\ntext4".normalizeLineSeparator("\n")
        )
    }
}
