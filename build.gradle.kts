import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
//    id("java")
    id("java-gradle-plugin")
    id("maven-publish")
    id("org.jetbrains.kotlin.jvm") version "1.3.72"
    id("com.gradle.plugin-publish") version "0.12.0"
    id("io.gitlab.arturbosch.detekt") version "1.9.1"
    id("com.github.breadmoirai.github-release") version "2.2.12"
}

description = "Gradle Changelog Plugin"
group = "org.jetbrains.intellij.plugins"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/jetbrains/markdown")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(gradleApi())
    implementation("org.jetbrains:markdown:0.1.42")
}

gradlePlugin {
    plugins.create("changelog") {
        id = "org.jetbrains.changelog"
        implementationClass = "org.jetbrains.changelog.ChangelogPlugin"
        displayName = "Gradle Changelog Plugin"
        description = "Provides tasks and helper methods for handling changelog in the Project."
    }
}

pluginBundle {
    website = "https://github.com/JetBrains/gradle-changelog-plugin"
    vcsUrl = "https://github.com/JetBrains/gradle-changelog-plugin.git"
    description = "Gradle Changelog Plugin"
    tags = listOf("changelog", "jetbrains")
}

tasks {
    listOf("compileKotlin", "compileTestKotlin").forEach {
        getByName<KotlinCompile>(it) {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
}
