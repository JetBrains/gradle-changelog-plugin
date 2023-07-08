// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

import java.text.SimpleDateFormat
import java.util.*
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
    fun `reformat changelog`() {
        for (lineSeparator: String in listOf("\n", "\r\n", "\r")) {
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
            
            """.trimIndent().normalizeLineSeparator(lineSeparator),
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
            """.trimIndent().normalizeLineSeparator(lineSeparator).reformat(lineSeparator),
                "reformat changelog that use $lineSeparator"
            )

            assertEquals(
                """
            Foo
            
            # My Title
            Introduction
            
            ## Upcoming version
            
            ### Added
            
            ### Removed
            
            """.trimIndent().normalizeLineSeparator(lineSeparator),
                """
            Foo
            # My Title
            Introduction
            ## Upcoming version
            ### Added
            ### Removed
            """.trimIndent().normalizeLineSeparator(lineSeparator).reformat(lineSeparator),
                "reformat changelog that use $lineSeparator"
            )

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
            
            [Unreleased] https://jetbrains.com/unreleased
            [1.0.0] https://jetbrains.com/1.0.0
            [0.1.0] https://jetbrains.com/0.1.0
            
            """.trimIndent().normalizeLineSeparator(lineSeparator),
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
            
            [Unreleased] https://jetbrains.com/unreleased
            [1.0.0] https://jetbrains.com/1.0.0
            [0.1.0] https://jetbrains.com/0.1.0
            """.trimIndent().normalizeLineSeparator(lineSeparator).reformat(lineSeparator),
                "reformat changelog that use $lineSeparator"
            )
        }
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

        assertEquals(
            text.replace("\n", "\r\n"),
            text.normalizeLineSeparator("\r\n")
        )

        assertEquals(
            text.replace("\n", "\r"),
            text.normalizeLineSeparator("\r")
        )
    }
}
