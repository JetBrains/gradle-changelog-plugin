// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.exceptions

class MissingFileException(path: String) : Exception("Changelog file does not exist: $path")
