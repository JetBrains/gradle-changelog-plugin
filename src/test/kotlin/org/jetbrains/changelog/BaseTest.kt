// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.changelog

import org.gradle.api.internal.project.DefaultProject
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.nio.file.Files.createTempDirectory
import kotlin.test.BeforeTest

open class BaseTest {

    protected lateinit var project: DefaultProject
    protected lateinit var extension: ChangelogPluginExtension

    private val gradleDefault = System.getProperty("test.gradle.default")
    private val gradleHome = System.getProperty("test.gradle.home")
    private val gradleArguments = System.getProperty("test.gradle.arguments", "").split(' ').filter(String::isNotEmpty).toMutableList()
    private val gradleVersion = System.getProperty("test.gradle.version").takeIf(String::isNotEmpty) ?: gradleDefault

    protected val changelogFile
        get() = File(extension.path.get())

    protected var changelog: String = ""
        set(value) {
            field = value
            changelogFile.run {
                createNewFile()
                writeText(value.trimIndent().trim())
            }
        }
        get() = changelogFile.readText()

    protected var version: String
        get() = project.version.toString()
        set(value) {
            project.version = value
        }

    protected var buildFile = ""
        set(value) {
            field = value
            File("${project.projectDir}/build.gradle").run {
                createNewFile()
                writeText(value.trimIndent())
            }
        }

    @BeforeTest
    fun setUp() {
        project = ProjectBuilder.builder()
            .withName("project")
            .withProjectDir(createTempDirectory("tmp").toFile()).build() as DefaultProject

        project.version = "1.0.0"
        project.plugins.apply(ChangelogPlugin::class.java)

        extension = project.extensions.getByType()
    }

    private fun prepareTask(taskName: String, vararg arguments: String) =
        GradleRunner.create()
            .withProjectDir(project.projectDir)
            .withGradleVersion(gradleVersion)
            .forwardOutput()
            .withPluginClasspath()
            .withTestKitDir(File(gradleHome))
            .withDebug(true)
            .withArguments(
                taskName,
                "--console=plain",
                "--stacktrace",
                "--configuration-cache",
                *arguments,
                *gradleArguments.toTypedArray()
            )

    protected fun runTask(taskName: String, vararg arguments: String): BuildResult =
        prepareTask(taskName, *arguments).build()

    protected fun runFailingTask(taskName: String, vararg arguments: String): BuildResult =
        prepareTask(taskName, *arguments).buildAndFail()
}
