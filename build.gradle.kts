import org.jetbrains.dokka.gradle.DokkaExtension

plugins {
    id("org.jetbrains.dokka")
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.android.gradle.plugin)
        classpath(libs.jetbrains.kotlin.gradle)
        classpath(libs.jetbrains.kotlin.serialization)
        classpath(libs.symbol.processing)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

// Multi-module Dokka: aggregate docs from all subprojects
dependencies {
    dokka(project(":annotations"))
    dokka(project(":api"))
    dokka(project(":android-assets"))
    dokka(project(":runtime"))
    dokka(project(":codegen-core"))
    dokka(project(":serialization-gson"))
    dokka(project(":serialization-kotlinx"))
    dokka(project(":library"))
}

dokka {
    dokkaPublications.html {
        outputDirectory.set(rootProject.file("dokka-docs"))
        failOnWarning.set(false)
    }
}

// Configure source links for subprojects that have Dokka applied
subprojects {
    plugins.withId("org.jetbrains.dokka") {
        extensions.configure(DokkaExtension::class.java) {
            val modulePath = project.path.removePrefix(":").replace(":", "/")

            dokkaSourceSets.configureEach {
                reportUndocumented.set(true)
                skipEmptyPackages.set(true)

                sourceLink {
                    localDirectory.set(layout.projectDirectory.dir("src/main/kotlin"))
                    remoteUrl.set(java.net.URI("https://github.com/AniTrend/retrofit-graphql/tree/develop/$modulePath/src/main/kotlin"))
                    remoteLineSuffix.set("#L")
                }
            }
        }
    }
}
