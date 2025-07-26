// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

plugins {
    id("com.gradle.develocity") version("4.1")
    id("com.autonomousapps.build-health") version("2.19.0")
    id("org.jetbrains.kotlin.jvm") version embeddedKotlinVersion apply false
}

rootProject.name = "gradle-changelog-plugin"

val isCI = (System.getenv("CI") ?: "false").toBoolean()

develocity {
    server = "https://ge.jetbrains.com"

    buildScan {
        termsOfUseAgree = "yes"
        publishing.onlyIf { isCI }
    }
}
