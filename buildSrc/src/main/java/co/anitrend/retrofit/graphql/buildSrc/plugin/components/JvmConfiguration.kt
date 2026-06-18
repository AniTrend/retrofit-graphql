package co.anitrend.retrofit.graphql.buildSrc.plugin.components

import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.libs
import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

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
