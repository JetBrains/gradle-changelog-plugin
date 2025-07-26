// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

import org.jetbrains.dokka.gradle.tasks.DokkaGeneratePublicationTask

fun Jar.patchManifest() = manifest { attributes("Version" to project.version) }

plugins {
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.pluginPublish)
    alias(libs.plugins.changelog)
    alias(libs.plugins.dokka)
    alias(libs.plugins.bcv)
}

group = providers.gradleProperty("projectGroup").get()
version = providers.gradleProperty("version").get()
description = providers.gradleProperty("description").get()

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.markdown) {
        exclude(group = "org.jetbrains.kotlin")
    }
    testImplementation(embeddedKotlin("test"))
    testImplementation(embeddedKotlin("test-junit"))
}

kotlin {
    jvmToolchain(11)
}

gradlePlugin {
    website = providers.gradleProperty("website")
    vcsUrl = providers.gradleProperty("vcsUrl")

    plugins.create("changelog") {
        id = providers.gradleProperty("pluginId").get()
        displayName = providers.gradleProperty("name").get()
        implementationClass = providers.gradleProperty("pluginImplementationClass").get()
        description = project.description
        tags = providers.gradleProperty("tags").map { it.split(',') }
    }
}

val dokkaGeneratePublicationHtml by tasks.existing(DokkaGeneratePublicationTask::class)
val javadocJar by tasks.registering(Jar::class) {
    dependsOn(dokkaGeneratePublicationHtml)
    archiveClassifier = "javadoc"
    from(dokkaGeneratePublicationHtml.map { it.outputDirectory })
    patchManifest()
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier = "sources"
    from(sourceSets.main.get().allSource)
    patchManifest()
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
        val testGradleHome = layout.buildDirectory.asFile.get().resolve("testGradleHome")

        doFirst {
            testGradleHome.mkdir()
        }

        systemProperties["test.gradle.home"] = testGradleHome
        systemProperties["test.gradle.default"] = providers.gradleProperty("gradleVersion").get()
        systemProperties["test.gradle.version"] = providers.gradleProperty("testGradleVersion").get()
        systemProperties["test.gradle.arguments"] = providers.gradleProperty("testGradleArguments").get()
        outputs.dir(testGradleHome)
    }

    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }
}
