// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

import org.jetbrains.dokka.gradle.DokkaTask

fun properties(key: String) = project.findProperty(key)?.toString()

plugins {
    `kotlin-dsl`
    `maven-publish`
    kotlin("jvm") version "1.7.20"
    id("com.gradle.plugin-publish") version "1.0.0"
    id("org.jetbrains.changelog") version "2.0.0"
    id("org.jetbrains.dokka") version "1.7.20"
}

version = properties("version")!!
group = properties("projectGroup")!!
description = properties("description")

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:markdown:0.3.1") {
        exclude(group = "org.jetbrains.kotlin")
    }
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}

kotlin {
    jvmToolchain(11)
}

gradlePlugin {
    website.set(properties("website"))
    vcsUrl.set(properties("vcsUrl"))

    plugins.create("changelog") {
        id = properties("pluginId")
        displayName = properties("pluginDisplayName")
        implementationClass = properties("pluginImplementationClass")
        description = project.description
        tags.set(properties("tags")?.split(','))
    }
}

val dokkaHtml by tasks.getting(DokkaTask::class)
val javadocJar by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
}

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

artifacts {
    archives(javadocJar)
    archives(sourcesJar)
}

changelog {
    groups.set(emptyList())
    repositoryUrl.set("https://github.com/JetBrains/gradle-changelog-plugin")
}

tasks {
    test {
        val testGradleHomePath = "$buildDir/testGradleHome"
        doFirst {
            File(testGradleHomePath).mkdir()
        }
        systemProperties["test.gradle.home"] = testGradleHomePath
        systemProperties["test.gradle.default"] = properties("gradleVersion")
        systemProperties["test.gradle.version"] = properties("testGradleVersion")
        systemProperties["test.gradle.arguments"] = properties("testGradleArguments")
        outputs.dir(testGradleHomePath)
    }

    wrapper {
        gradleVersion = properties("gradleVersion")
        distributionUrl = "https://cache-redirector.jetbrains.com/services.gradle.org/distributions/gradle-$gradleVersion-all.zip"
    }
}
