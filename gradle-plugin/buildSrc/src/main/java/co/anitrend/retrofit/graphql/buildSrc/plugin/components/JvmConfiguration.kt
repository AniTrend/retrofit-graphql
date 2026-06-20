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

/**
 * Stub for the composite build. Dokka is only needed in the root build
 * where API docs are generated; this avoids adding a Dokka dependency
 * to the composite build's buildSrc.
 */
fun Project.configureDokkaForJvm() {
    // No-op: composite build does not generate Dokka for codegen-core
}

/**
 * Stub for the composite build. Maven publishing is only configured
 * in the root build where JitPack artifacts are produced.
 */
fun Project.configureJvmPublishing() {
    // No-op: composite build does not publish codegen-core
}
