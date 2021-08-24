package org.jetbrains.changelog.exceptions

class HeaderParseException(value: String, unreleasedTerm: String) : Exception(
    "Header '$value' does not contain version number. " + (
        "Probably you want set unreleasedTerm to '$value'"
            .takeIf { value.contains(unreleasedTerm) } ?: ""
        )
)
