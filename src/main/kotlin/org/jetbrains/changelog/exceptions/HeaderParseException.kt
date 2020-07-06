package org.jetbrains.changelog.exceptions

import org.jetbrains.changelog.ChangelogPluginExtension

class HeaderParseException(value: String, extension: ChangelogPluginExtension) : Exception(
    "Header '$value' cannot be parsed with the following " +
        "headerMessageFormat: '${extension.headerMessageFormat().toPattern()}'. " +
        ("Probably you want set unreleasedTerm to '$value'".takeIf { value.contains(extension.unreleasedTerm) } ?: "")
)
