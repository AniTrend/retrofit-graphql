package co.anitrend.retrofit.graphql.buildSrc.plugin.strategy

import org.gradle.api.Project
import co.anitrend.retrofit.graphql.buildSrc.Libraries
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.isSampleModule
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.implementation
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.androidTest
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.test
import org.gradle.api.artifacts.dsl.DependencyHandler

internal class DependencyStrategy(
    private val project: Project
) {

    private fun DependencyHandler.applyDefaultDependencies() {
        implementation(Libraries.JetBrains.Kotlin.stdlib)

        if (project.isSampleModule()) {
            implementation(Libraries.JetBrains.Kotlin.reflect)
            implementation(Libraries.Koin.AndroidX.fragment)
            implementation(Libraries.Koin.AndroidX.viewmodel)
            implementation(Libraries.Koin.AndroidX.scope)
            implementation(Libraries.Koin.extension)
            implementation(Libraries.Koin.core)
        }

        // Testing libraries
        test(Libraries.junit)
        test(Libraries.mockk)
    }

    private fun DependencyHandler.applyTestDependencies() {
        if (project.isSampleModule())
            test(Libraries.Koin.test)
        androidTest(Libraries.AndroidX.Test.core)
        androidTest(Libraries.AndroidX.Test.runner)
        androidTest(Libraries.AndroidX.Test.Espresso.core)
    }

    private fun DependencyHandler.applyLifeCycleDependencies() {
        implementation(Libraries.AndroidX.Lifecycle.liveDataCoreKtx)
        implementation(Libraries.AndroidX.Lifecycle.viewModelKtx)
        implementation(Libraries.AndroidX.Lifecycle.liveDataKtx)
        implementation(Libraries.AndroidX.Lifecycle.runTimeKtx)
        implementation(Libraries.AndroidX.Lifecycle.extensions)
    }

    private fun DependencyHandler.applyNetworkingDependencies() {
        implementation(Libraries.Square.Retrofit.retrofit)
        implementation(Libraries.Square.Retrofit.gsonConverter)
        if (project.isSampleModule())
            implementation(Libraries.Square.OkHttp.logging)
    }

    fun applyDependenciesOn(handler: DependencyHandler) {
        handler.applyDefaultDependencies()
        handler.applyNetworkingDependencies()
        handler.applyTestDependencies()
        if (project.isSampleModule())
            handler.applyLifeCycleDependencies()
    }
}