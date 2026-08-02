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
     * Applies Retrofit and OkHttp as [api] dependencies.
     * Used by :runtime whose public API signatures expose
     * [retrofit2.Converter.Factory], [retrofit2.Retrofit], [okhttp3.RequestBody],
     * and [okhttp3.ResponseBody]. No JSON backend is exposed: the new
     * codec-backed converter path is backend-neutral.
     */
    private fun DependencyHandler.applyRetrofitDependencies() {
        api(project.libs.square.okhttp)
        api(project.libs.square.retrofit)
    }

    /**
     * Applies Gson and converter-gson as [api] dependencies on top of the
     * Retrofit/OkHttp pair. Used by :compat whose legacy public API exposes
     * [com.google.gson.Gson] (factory overloads, GsonGraphQLJson,
     * PersistedQueryUrlParameterBuilder) and the Gson-backed GraphConverter
     * default path.
     */
    private fun DependencyHandler.applyGsonRetrofitDependencies() {
        api(project.libs.gson)
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
            // :runtime exposes Retrofit and OkHttp in the codec-backed
            // GraphQLConverterFactory and converters. No Gson or backend
            // dependency: serialization is delegated to GraphQLTransportCodec.
            Modules.Components.Runtime.id -> handler.applyRetrofitDependencies()

            // :compat exposes Retrofit, OkHttp, Gson, and converter-gson in
            // the legacy GraphConverter, GraphRequestConverter,
            // GraphResponseConverter, and GraphErrorUtil public API.
            Modules.Components.Compat.id -> {
                handler.applyRetrofitDependencies()
                handler.applyGsonRetrofitDependencies()
            }

            // :app consumes all dependencies from above modules but uses
            // implementation scope to avoid leaking them transitively
            Modules.Components.App.id -> handler.applyRetrofitAsImplementation()

            Modules.Components.Api.id,
            Modules.Components.AndroidAssets.id,
            Modules.Components.Library.id,
            Modules.Components.SerializationApi.id,
            Modules.Components.SerializationGson.id,
            Modules.Components.SerializationKotlinx.id,
            -> { /* Dependencies handled in module build.gradle.kts */ }
        }
    }
}
