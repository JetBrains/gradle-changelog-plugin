// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

import org.jetbrains.dokka.gradle.DokkaTask

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.kotlin)
    alias(libs.plugins.pluginPublish)
    alias(libs.plugins.changelog)
    alias(libs.plugins.dokka)
}

group = properties("projectGroup").get()
version = properties("version").get()
description = properties("description").get()

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.markdown) {
        exclude(group = "org.jetbrains.kotlin")
    }
    testImplementation(libs.kotlinTest)
    testImplementation(libs.kotlinTestJunit)
}

kotlin {
    jvmToolchain(11)
}

@Suppress("UnstableApiUsage")
gradlePlugin {
    website = properties("website")
    vcsUrl = properties("vcsUrl")

    plugins.create("changelog") {
        id = properties("pluginId").get()
        displayName = properties("name").get()
        implementationClass = properties("pluginImplementationClass").get()
        description = project.description
        tags = properties("tags").map { it.split(',') }
    }
}

val dokkaHtml by tasks.getting(DokkaTask::class)
val javadocJar by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier = "javadoc"
    from(dokkaHtml.outputDirectory)
}

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    archiveClassifier = "sources"
    from(sourceSets.main.get().allSource)
}

artifacts {
    archives(javadocJar)
    archives(sourcesJar)
}

changelog {
    groups = emptyList()
    repositoryUrl = "https://github.com/JetBrains/gradle-changelog-plugin"
}

tasks {
    test {
        val testGradleHomePath = "$buildDir/testGradleHome"
        doFirst {
            File(testGradleHomePath).mkdir()
        }
        systemProperties["test.gradle.home"] = testGradleHomePath
        systemProperties["test.gradle.default"] = properties("gradleVersion").get()
        systemProperties["test.gradle.version"] = properties("testGradleVersion").get()
        systemProperties["test.gradle.arguments"] = properties("testGradleArguments").get()
        outputs.dir(testGradleHomePath)
    }

    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }
}
