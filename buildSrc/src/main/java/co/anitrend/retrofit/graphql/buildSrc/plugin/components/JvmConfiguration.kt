package co.anitrend.retrofit.graphql.buildSrc.plugin.components

import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.libs
import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.dokka.gradle.DokkaExtension

fun Project.configureSpotlessForJvm() {
    plugins.apply("com.diffplug.spotless")
    configure<SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude(
                "${layout.buildDirectory.get()}/**/*.kt",
                "**/test/**/*.kt",
                "bin/**/*.kt"
            )
            ktlint(libs.pintrest.ktlint.get().version)
                .editorConfigOverride(mapOf("ktlint_standard_filename" to "disabled"))
            licenseHeaderFile(rootProject.file("spotless/copyright.kt"))
        }
    }
}

fun Project.configureDokkaForJvm() {
    plugins.apply("org.jetbrains.dokka")
    extensions.configure<DokkaExtension> {
        moduleName.set("retrofit-graphql-${this@configureDokkaForJvm.name}")
        dokkaSourceSets.configureEach {
            skipDeprecated.set(false)
            reportUndocumented.set(true)
            skipEmptyPackages.set(true)
            jdkVersion.set(21)
            sourceRoots.from(file("src"))
        }
    }
}
