package org.jetbrains.changelog

import kotlin.test.Test
import kotlin.test.assertEquals

class ExtensionsTest {
    @Test
    fun closureTest() {
        val c = closure { "response" }

        assertEquals(c.call(), "response")
    }
}
