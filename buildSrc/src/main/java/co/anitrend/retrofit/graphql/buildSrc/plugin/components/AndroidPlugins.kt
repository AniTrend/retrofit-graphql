package co.anitrend.retrofit.graphql.buildSrc.plugin.components

import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.isLibraryModule
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.isSampleModule
import org.gradle.api.Project

private fun Project.applyModulePlugin() {
    if (isLibraryModule()) {
        plugins.apply("com.android.library")
        plugins.apply("org.jetbrains.dokka")
        plugins.apply("com.diffplug.spotless")
        plugins.apply("maven-publish")
    }
    else
        plugins.apply("com.android.application")
}

internal fun Project.configurePlugins() {
    applyModulePlugin()
    // kotlin-android no longer needed: AGP 9.0+ provides built-in Kotlin support
    if (isSampleModule())
        plugins.apply("com.google.devtools.ksp")
}