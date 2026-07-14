import java.util.Properties

pluginManagement {
    plugins {
        kotlin("jvm") version "2.4.10"
    }
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "gradle-plugin"

include(":codegen-core")
project(":codegen-core").projectDir = file("../codegen-core")

val standaloneVersionProperties = Properties().apply {
    file("../gradle/version.properties").inputStream().use(::load)
}

gradle.beforeProject {
    group = "co.anitrend"
    version = standaloneVersionProperties.getProperty("version")
}
