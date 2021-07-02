import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key)?.toString()

plugins {
    id("java-gradle-plugin")
    id("maven-publish")
    id("org.jetbrains.changelog") version "1.1.2"
    id("org.jetbrains.kotlin.jvm") version "1.5.20"
    id("com.gradle.plugin-publish") version "0.15.0"
    id("io.gitlab.arturbosch.detekt") version "1.17.1"
    id("org.jlleitschuh.gradle.ktlint") version "10.1.0"
}

description = properties("description")
group = properties("projectGroup")!!
version = properties("version")!!

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:markdown:0.2.4") {
        exclude(group = "org.jetbrains.kotlin")
    }
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.17.1")
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
    version = "${project.version}"
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
            kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
        }
    }

    withType<Detekt>().configureEach {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
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
