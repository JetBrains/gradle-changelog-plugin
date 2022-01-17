rootProject.name = "gradle-changelog-plugin"

plugins {
    id("com.gradle.enterprise") version("3.8.1")
}

gradleEnterprise {
    server = "http://ge.labs.jb.gg"
}
