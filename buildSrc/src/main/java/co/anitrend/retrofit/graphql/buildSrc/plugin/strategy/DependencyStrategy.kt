package co.anitrend.retrofit.graphql.buildSrc.plugin.strategy

import co.anitrend.retrofit.graphql.buildSrc.Libraries
import co.anitrend.retrofit.graphql.buildSrc.common.sample
import org.gradle.api.artifacts.dsl.DependencyHandler

internal class DependencyStrategy(
    private val module: String
) {

    private fun DependencyHandler.applyDefaultDependencies() {
        add("implementation", Libraries.JetBrains.Kotlin.stdlib)

        if (module == sample) {
            add("implementation", Libraries.JetBrains.Kotlin.reflect)
            add("implementation", Libraries.Koin.AndroidX.fragment)
            add("implementation", Libraries.Koin.AndroidX.viewmodel)
            add("implementation", Libraries.Koin.AndroidX.scope)
            add("implementation", Libraries.Koin.extension)
            add("implementation", Libraries.Koin.core)
        }

        // Testing libraries
        add("testImplementation", Libraries.junit)
        add("testImplementation", Libraries.mockk)
    }

    private fun DependencyHandler.applyTestDependencies() {
        if (module == sample)
            add("testImplementation", Libraries.Koin.test)
        add("androidTestImplementation", Libraries.AndroidX.Test.core)
        add("androidTestImplementation", Libraries.AndroidX.Test.runner)
        add("androidTestImplementation", Libraries.AndroidX.Test.Espresso.core)
    }

    private fun DependencyHandler.applyLifeCycleDependencies() {
        add("implementation", Libraries.AndroidX.Lifecycle.liveDataCoreKtx)
        add("implementation", Libraries.AndroidX.Lifecycle.viewModelKtx)
        add("implementation", Libraries.AndroidX.Lifecycle.liveDataKtx)
        add("implementation", Libraries.AndroidX.Lifecycle.runTimeKtx)
        add("implementation", Libraries.AndroidX.Lifecycle.extensions)
    }

    private fun DependencyHandler.applyNetworkingDependencies() {
        add("implementation", Libraries.Square.Retrofit.retrofit)
        add("implementation", Libraries.Square.Retrofit.gsonConverter)
        if (module == sample)
            add("implementation", Libraries.Square.OkHttp.logging)
    }

    fun applyDependenciesOn(handler: DependencyHandler) {
        handler.applyDefaultDependencies()
        handler.applyNetworkingDependencies()
        handler.applyTestDependencies()
        if (module == sample)
            handler.applyLifeCycleDependencies()
    }
}