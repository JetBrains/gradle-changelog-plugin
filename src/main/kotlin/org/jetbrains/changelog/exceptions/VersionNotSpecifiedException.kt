// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.exceptions

class VersionNotSpecifiedException : Exception(
    "Version is missing. Please provide the project version to the `project` " +
        "or `changelog.version` property explicitly."
)
