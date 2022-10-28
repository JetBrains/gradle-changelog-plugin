// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.ChangelogPluginExtension

abstract class BaseChangelogTask : DefaultTask() {

    /**
     * Unreleased section name, see [ChangelogPluginExtension.unreleasedTerm].
     */
    @get:Internal
    abstract val unreleasedTerm: Property<String>

    /**
     * [Changelog] instance shared between [ChangelogPluginExtension] and tasks.
     */
    @get:Internal
    abstract val changelog: Property<Changelog>

}
