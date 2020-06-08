package org.jetbrains.changelog.exceptions

class MissingVersionException(version: String) : Exception("Version has no changelog: $version")
