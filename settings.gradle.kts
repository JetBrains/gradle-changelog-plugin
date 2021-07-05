rootProject.name = "gradle-changelog-plugin"

plugins {
    id("com.gradle.enterprise") version("3.6.3")
}

gradleEnterprise {
    server = "http://ge.labs.jb.gg"
}
