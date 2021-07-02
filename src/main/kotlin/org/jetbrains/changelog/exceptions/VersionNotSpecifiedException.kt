package org.jetbrains.changelog.exceptions

class VersionNotSpecifiedException : Exception(
    "Version is missing. Please provide the project version to the `project` " +
        "or `changelog.version` property explicitly."
)
