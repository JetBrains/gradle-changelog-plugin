package org.jetbrains.changelog.exceptions

class VersionNotSpecifiedException : Exception(
    "Changelog version wasn't provided." +
        "Please specify the value for the `changelog.version` property explicitly."
)
