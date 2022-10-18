// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.exceptions

class HeaderParseException(value: String, unreleasedTerm: String) : Exception(
    "Header '$value' does not contain version number. "
            + "By default, SemVer format is required (i.e. 1.0.0). To use other formats, like '1.0', adjust the 'changelog.headerParserRegex' property. "
            + ("Probably you want set unreleasedTerm to '$value'".takeIf { value.contains(unreleasedTerm) } ?: "")
)
