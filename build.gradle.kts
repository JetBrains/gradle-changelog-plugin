import groovy.util.Node
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-gradle-plugin")
    id("maven-publish")
    id("org.jetbrains.changelog") version "1.0.0"
    id("org.jetbrains.kotlin.jvm") version "1.4.30"
    id("com.gradle.plugin-publish") version "0.12.0"
    id("io.gitlab.arturbosch.detekt") version "1.15.0"
    id("org.jlleitschuh.gradle.ktlint") version "9.4.1"
    id("com.github.breadmoirai.github-release") version "2.2.12"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

description = "Gradle Changelog Plugin"
group = "org.jetbrains.intellij.plugins"
version = "1.0.1"

repositories {
    mavenCentral()
    jcenter()
    maven("https://dl.bintray.com/jetbrains/markdown")
}

dependencies {
    shadow(localGroovy())
    shadow(gradleApi())
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains:markdown:0.1.45")
    implementation("org.jetbrains.kotlinx:kotlinx-html-assembly:0.7.2")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.15.0")
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
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
    website = "https://github.com/JetBrains/${project.name}"
    vcsUrl = "https://github.com/JetBrains/${project.name}.git"
    description = "Gradle Changelog Plugin"
    tags = listOf("changelog", "jetbrains")
}

detekt {
    config.from(file("detekt.yml"))
    buildUponDefaultConfig = true
    parallel = true

    reports {
        html.enabled = false
        xml.enabled = false
        txt.enabled = false
    }
}

tasks {
    listOf("compileKotlin", "compileTestKotlin").forEach {
        getByName<KotlinCompile>(it) {
            kotlinOptions.jvmTarget = "1.8"
        }
    }

    withType<Detekt>().configureEach {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    shadowJar {
        archiveClassifier.set("")
        dependencies { include { it.moduleName == "markdown" } }
    }

    jar {
        enabled = false
        dependsOn(shadowJar)
    }
}

publishing {
    publications.create<MavenPublication>("pluginMaven") {
        artifact(tasks.shadowJar)

        // Hack for removing the org.jetbrains:markdown:0.1.41 dependency from the generated POM file.
        // Somehow shadowJar does not alter the dependencies list.
        pom.withXml {
            val node = (asNode().depthFirst()).find {
                (it as Node).text() == "markdown"
            } as Node
            node.parent().apply { parent().remove(this) }
        }
    }
}
