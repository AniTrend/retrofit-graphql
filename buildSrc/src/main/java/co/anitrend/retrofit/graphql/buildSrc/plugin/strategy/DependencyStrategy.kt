package co.anitrend.retrofit.graphql.buildSrc.plugin.strategy

import org.gradle.api.Project
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.isSampleModule
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.isAnnotationsModule
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.isFacadeModule
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.implementation
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.androidTest
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.libs
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.test
import co.anitrend.retrofit.graphql.buildSrc.module.Modules
import org.gradle.api.artifacts.dsl.DependencyHandler

internal class DependencyStrategy(
    private val project: Project
) {

    private fun DependencyHandler.applyDefaultDependencies() {
        implementation(project.libs.jetbrains.kotlin.stdlib)

        if (project.isSampleModule()) {
            implementation(project.libs.jetbrains.kotlin.reflect)
            implementation(project.libs.koin.core)
            implementation(project.libs.koin.android)
        }

        // Testing libraries
        test(project.libs.junit)
        test(project.libs.mockk)
    }

    private fun DependencyHandler.applyNetworkingDependencies() {
        implementation(project.libs.square.retrofit)
        implementation(project.libs.square.retrofit.gson.converter)
    }

    fun applyDependenciesOn(handler: DependencyHandler) {
        handler.applyDefaultDependencies()

        // Only apply networking dependencies to modules that need Retrofit/Gson
        when (project.name) {
            Modules.Components.Runtime.id,
            Modules.Components.Api.id,
            -> handler.applyNetworkingDependencies()
            Modules.Components.AndroidAssets.id,
            Modules.Components.Library.id,
            Modules.Components.SerializationGson.id,
            Modules.Components.SerializationKotlinx.id,
            -> { /* No networking deps needed */ }
        }
    }
}