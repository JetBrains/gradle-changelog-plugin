// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.exceptions

class HeaderParseException(value: String, unreleasedTerm: String) : Exception(
    "Header '$value' does not contain version number. " + (
        "Probably you want set unreleasedTerm to '$value'"
            .takeIf { value.contains(unreleasedTerm) } ?: ""
        )
)
