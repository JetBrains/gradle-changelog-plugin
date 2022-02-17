import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key)?.toString()

plugins {
    `kotlin-dsl`
    `maven-publish`
    id("org.jetbrains.changelog") version "1.3.1"
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
    id("com.gradle.plugin-publish") version "0.20.0"
}

description = properties("description")
group = properties("projectGroup")!!
version = properties("version")!!

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

gradlePlugin {
    plugins.create("changelog") {
        id = properties("pluginId")
        implementationClass = properties("pluginImplementationClass")
        displayName = properties("pluginDisplayName")
        description = properties("pluginDescription")
    }
}

pluginBundle {
    website = properties("website")
    vcsUrl = properties("vcsUrl")
    description = properties("description")
    tags = properties("tags")?.split(',')
}

changelog {
    version.set("${project.version}")
    groups.set(emptyList())
}

tasks {
    listOf("compileKotlin", "compileTestKotlin").forEach {
        getByName<KotlinCompile>(it) {
            kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
        }
    }

    wrapper {
        gradleVersion = properties("gradleVersion")
        distributionUrl = "https://cache-redirector.jetbrains.com/services.gradle.org/distributions/gradle-$gradleVersion-all.zip"
    }

    test {
        val testGradleHomePath = "$buildDir/testGradleHome"
        doFirst {
            File(testGradleHomePath).mkdir()
        }
        systemProperties["test.gradle.home"] = testGradleHomePath
        systemProperties["test.gradle.default"] = properties("gradleVersion")
        systemProperties["test.gradle.version"] = properties("testGradleVersion")
    }
}
