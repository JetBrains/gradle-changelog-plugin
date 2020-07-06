package org.jetbrains.changelog

import org.gradle.api.internal.project.DefaultProject
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.test.BeforeTest

open class BaseTest {

    protected lateinit var project: DefaultProject
    protected lateinit var extension: ChangelogPluginExtension

    protected var changelog: String = ""
        set(value) {
            field = value
            File(extension.path).run {
                createNewFile()
                writeText(value.trimIndent().trim())
            }
        }

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
            .withProjectDir(createTempDir()).build() as DefaultProject

        project.plugins.apply(ChangelogPlugin::class.java)

        extension = project.extensions.getByType(ChangelogPluginExtension::class.java)
    }

    protected fun runTask(taskName: String, vararg arguments: String): BuildResult = GradleRunner.create()
            .withProjectDir(project.projectDir)
            .withArguments(taskName, "--console=plain", "-q", "--stacktrace", *arguments)
            .withPluginClasspath()
            .build()
}
