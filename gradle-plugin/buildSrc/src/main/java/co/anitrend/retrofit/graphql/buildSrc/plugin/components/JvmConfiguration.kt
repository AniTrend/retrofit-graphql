package co.anitrend.retrofit.graphql.buildSrc.plugin.components

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Minimal stub for the composite build. Configures spotless for JVM modules
 * using a project-relative copyright file when available.
 */
fun Project.configureSpotlessForJvm() {
    plugins.apply("com.diffplug.spotless")
    configure<SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude(
                "${layout.buildDirectory.get()}/**/*.kt",
                "**/test/**/*.kt",
                "bin/**/*.kt",
            )
            ktlint("1.0.1")
                .editorConfigOverride(mapOf("ktlint_standard_filename" to "disabled"))
            val copyrightFile = rootProject.file("../../spotless/copyright.kt")
            if (copyrightFile.exists()) {
                licenseHeaderFile(copyrightFile)
            }
        }
    }
}
