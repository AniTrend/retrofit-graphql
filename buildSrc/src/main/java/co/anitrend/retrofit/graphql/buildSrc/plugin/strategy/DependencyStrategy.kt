package co.anitrend.retrofit.graphql.buildSrc.plugin.strategy

import org.gradle.api.Project
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.isSampleModule
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.api
import co.anitrend.retrofit.graphql.buildSrc.plugin.extensions.implementation
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

    /**
     * Applies Gson as an [api] dependency. Used by library modules
     * whose public API signatures expose [com.google.gson.Gson] directly
     * (e.g. [PersistedQueryUrlParameterBuilder] in :api).
     *
     * :app receives Gson as [implementation] instead.
     */
    private fun DependencyHandler.applyGsonDependency() {
        api(project.libs.gson)
    }

    /**
     * Applies Gson, Retrofit, OkHttp, and converter-gson as [api] dependencies.
     * Used by :runtime whose public API signatures expose
     * [retrofit2.Converter.Factory], [retrofit2.Retrofit], [okhttp3.RequestBody],
     * [okhttp3.ResponseBody], and [com.google.gson.Gson].
     *
     * :app receives all three as [implementation] instead.
     */
    private fun DependencyHandler.applyRetrofitDependencies() {
        api(project.libs.gson)
        api(project.libs.square.okhttp)
        api(project.libs.square.retrofit)
        api(project.libs.square.retrofit.gson.converter)
    }

    /**
     * Applies Gson, Retrofit, OkHttp, and converter-gson as [implementation] dependencies.
     * Used by :app so its compile classpath is not polluted by
     * transitive Retrofit/OkHttp/Gson declarations from library modules.
     */
    private fun DependencyHandler.applyRetrofitAsImplementation() {
        implementation(project.libs.gson)
        implementation(project.libs.square.okhttp)
        implementation(project.libs.square.retrofit)
        implementation(project.libs.square.retrofit.gson.converter)
    }

    fun applyDependenciesOn(handler: DependencyHandler) {
        handler.applyDefaultDependencies()

        when (project.name) {
            // :api exposes Gson in PersistedQueryUrlParameterBuilder constructor
            Modules.Components.Api.id -> handler.applyGsonDependency()

            // :runtime exposes Retrofit, OkHttp, converter-gson, and Gson in
            // GraphConverter, GraphRequestConverter, GraphResponseConverter
            Modules.Components.Runtime.id -> handler.applyRetrofitDependencies()

            // :app consumes all dependencies from above modules but uses
            // implementation scope to avoid leaking them transitively
            Modules.Components.App.id -> handler.applyRetrofitAsImplementation()

            Modules.Components.AndroidAssets.id,
            Modules.Components.Library.id,
            Modules.Components.SerializationGson.id,
            Modules.Components.SerializationKotlinx.id,
            -> { /* Dependencies handled in module build.gradle.kts */ }
        }
    }
}
