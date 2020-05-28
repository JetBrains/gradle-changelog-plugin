package org.jetbrains.changelog.exceptions

class MissingVersionException(version: String) : Exception("Version '$version' has no changelog")
