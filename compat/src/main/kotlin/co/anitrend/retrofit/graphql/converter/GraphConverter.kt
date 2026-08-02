/**
 * Copyright 2021 AniTrend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.anitrend.retrofit.graphql.converter

import android.content.Context
import co.anitrend.retrofit.graphql.annotation.GraphQuery
import co.anitrend.retrofit.graphql.annotation.processor.GraphProcessor
import co.anitrend.retrofit.graphql.annotation.processor.contract.AbstractGraphProcessor
import co.anitrend.retrofit.graphql.annotation.processor.fragment.FragmentPatcher
import co.anitrend.retrofit.graphql.annotation.processor.plugin.AssetManagerDiscoveryPlugin
import co.anitrend.retrofit.graphql.converter.request.GraphRequestConverter
import co.anitrend.retrofit.graphql.converter.response.GraphResponseConverter
import co.anitrend.retrofit.graphql.logger.DefaultGraphLogger
import co.anitrend.retrofit.graphql.logger.contract.ILogger
import co.anitrend.retrofit.graphql.logger.core.AbstractLogger
import co.anitrend.retrofit.graphql.model.GraphQLDocumentRegistry
import co.anitrend.retrofit.graphql.model.GraphQLJson
import co.anitrend.retrofit.graphql.serialization.gson.GsonGraphQLJson
import co.anitrend.retrofit.graphql.util.LogLevel
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

/**
 * Body for GraphQL requests and responses, closed for modification
 * but open for extension.
 *
 * **Legacy**: this Gson/GraphQLJson-backed converter lives in `:compat` for
 * backward compatibility with the historical artifact and the asset-based
 * discovery flow. New code should use the backend-neutral
 * [GraphQLConverterFactory] from `:runtime` with an explicit
 * [co.anitrend.retrofit.graphql.serialization.GraphQLTransportCodec].
 *
 * ## Serialization Backend
 *
 * As of v0.13.x, [GraphConverter] uses a pluggable [GraphQLJson] instance
 * instead of a hard dependency on Gson. The [json] property provides
 * the serialization backend for both request and response conversion.
 * Factory methods accept either a [GraphQLJson] instance directly or
 * a [Gson] instance (preserved for backward compatibility, internally
 * wrapped in [GsonGraphQLJson]).
 *
 * ## Factory Methods
 *
 * ### Context-backed (5 overloads: 4 Gson preserved + 1 GraphQLJson new)
 *
 * ```kotlin
 * // Default Gson (backward compatible)
 * GraphConverter.create(context)
 * // Custom Gson (backward compatible)
 * GraphConverter.create(context, gson)
 * // Custom Gson + registry (backward compatible)
 * GraphConverter.create(context, gson, registry)
 * // Default Gson + registry (backward compatible)
 * GraphConverter.create(context, registry)
 * // NEW: Custom GraphQLJson + registry
 * GraphConverter.create(context, json, registry)
 * ```
 *
 * ### Registry-only, no Context (3 overloads: 2 Gson preserved + 1 GraphQLJson new)
 *
 * ```kotlin
 * // Default Gson (backward compatible)
 * GraphConverter.create(registry)
 * // Custom Gson (backward compatible)
 * GraphConverter.create(gson, registry)
 * // NEW: Custom GraphQLJson
 * GraphConverter.create(json, registry)
 * ```
 *
 * ## Extending
 *
 * Subclass [GraphConverter] to customize request/response conversion:
 * ```kotlin
 * class CustomGraphConverter(
 *     processor: AbstractGraphProcessor,
 *     json: GraphQLJson,
 * ) : GraphConverter(processor, json) {
 *     override fun responseBodyConverter(...) = CustomResponseConverter(...)
 * }
 * ```
 *
 * @param graphProcessor Processor used for asset-based lookup. Registry-only factory overloads
 *   provide a no-op implementation when no asset fallback is required.
 * @param json Pluggable [GraphQLJson] instance used for request and response serialization.
 *   Replaces the v2.x `gson: Gson` parameter.
 * @param registry Optional [GraphQLDocumentRegistry] for build-time generated operation documents.
 *
 * @see GraphQLConverterFactory
 * @see co.anitrend.retrofit.graphql.serialization.GraphQLTransportCodec
 */
@Deprecated(
    "Legacy Gson/GraphQLJson-backed converter. Use GraphQLConverterFactory with an explicit GraphQLTransportCodec instead.",
)
open class GraphConverter(
    protected val graphProcessor: AbstractGraphProcessor,
    protected val json: GraphQLJson,
    protected val registry: GraphQLDocumentRegistry? = null,
) : Converter.Factory() {
    /**
     * Response body converter delegates logic processing to a child class that handles
     * wrapping and deserialization of the json response data.
     *
     * @param annotations All the annotation applied to the requesting method
     * @param retrofit The retrofit object representing the response
     * @param type The generic type declared on the method
     *
     * @see GraphResponseConverter
     */
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<ResponseBody, *>? {
        return when (type) {
            is ResponseBody -> super.responseBodyConverter(type, annotations, retrofit)
            else -> GraphResponseConverter<Any>(type, json)
        }
    }

    /**
     * Response body converter delegates logic processing to a child class that handles
     * wrapping and deserialization of the json response data.
     *
     * @param parameterAnnotations All the annotation applied to request parameters
     * @param methodAnnotations All the annotation applied to the requesting method
     * @param retrofit The retrofit object representing the response
     * @param type The type of the parameter of the request
     *
     * @see GraphRequestConverter
     */
    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<*, RequestBody>? {
        return GraphRequestConverter(methodAnnotations, graphProcessor, json, registry, type)
    }

    /**
     * Sets the minimum level for log messages. Attempted messages with a too low
     * log level are skipped and not printed to the system log.
     *
     * @param logLevel The minimum log level used to print log messages
     */
    @Deprecated(
        "Use setMinimumLogLevel instead or alternatively setup your on logger on GraphConverter.create",
        ReplaceWith(
            "setMinimumLogLevel(ILogger.Level)",
            "co.anitrend.retrofit.graphql.logger.contract.ILogger.Level",
        ),
        level = DeprecationLevel.ERROR,
    )
    fun setLogLevel(logLevel: LogLevel) {
        val level =
            when (logLevel) {
                LogLevel.DEBUG -> ILogger.Level.DEBUG
                LogLevel.ERROR -> ILogger.Level.ERROR
                LogLevel.INFO -> ILogger.Level.INFO
                LogLevel.VERBOSE -> ILogger.Level.VERBOSE
                LogLevel.WARN -> ILogger.Level.WARNING
                else -> ILogger.Level.NONE
            }
        setMinimumLogLevel(level)
    }

    /**
     * Overrides the minimum level for log messages on the logger.
     *
     * @param level The minimum log level
     */
    fun setMinimumLogLevel(level: ILogger.Level) {
        graphProcessor.logger.level = level
    }

    companion object {
        const val MIME_TYPE = "application/graphql"

        private fun defaultGson(): Gson =
            GsonBuilder()
                .enableComplexMapKeySerialization()
                .serializeNulls()
                .setLenient()
                .create()

        private fun defaultJson(): GraphQLJson = GsonGraphQLJson(defaultGson())

        private fun assetBackedProcessor(
            context: Context,
            level: ILogger.Level,
        ): AbstractGraphProcessor =
            GraphProcessor(
                AssetManagerDiscoveryPlugin(context.assets),
                DefaultGraphLogger(level),
            )

        private fun registryOnlyProcessor(level: ILogger.Level): AbstractGraphProcessor =
            object : AbstractGraphProcessor() {
                override val defaultExtension: String = ".graphql"
                override val defaultDirectory: String = "graphql"
                override val logger: AbstractLogger = DefaultGraphLogger(level)
                override val fragmentPatcher: FragmentPatcher = FragmentPatcher(defaultExtension, logger = logger)
                override val graphFiles: Map<String, String> = emptyMap()

                override fun getQuery(annotations: Array<out Annotation>): String? {
                    // Keep this extraction logic in sync with GraphRequestConverter.extractOperationName().
                    val operationName =
                        annotations.filterIsInstance<GraphQuery>()
                            .firstOrNull()
                            ?.value
                            ?.takeIf { it.isNotEmpty() }

                    if (operationName != null) {
                        throw IllegalStateException(
                            "GraphQL operation '$operationName' was not found in the registry.",
                        )
                    }

                    return null
                }

                override fun patchQueries() = Unit
            }

        // -------------------------------------------------------
        // Factory methods with Context (asset-backed processor)
        // -------------------------------------------------------

        /**
         * Default creator that uses a predefined Gson configuration.
         *
         * @param context A valid application context
         * @param level Minimum log level
         */
        @JvmOverloads
        fun create(
            context: Context,
            level: ILogger.Level = ILogger.Level.INFO,
        ): GraphConverter =
            GraphConverter(
                graphProcessor = assetBackedProcessor(context, level),
                json = defaultJson(),
            )

        /**
         * Allows you to provide your own [Gson] configuration which will be used when serialize or
         * deserialize response and request bodies.
         *
         * @param context A valid application context
         * @param gson Custom gson implementation
         * @param level Minimum log level
         */
        @JvmOverloads
        fun create(
            context: Context,
            gson: Gson,
            level: ILogger.Level = ILogger.Level.INFO,
        ): GraphConverter =
            GraphConverter(
                graphProcessor = assetBackedProcessor(context, level),
                json = GsonGraphQLJson(gson),
            )

        /**
         * Creates a [GraphConverter] with the default [GraphQLJson] and a build-time
         * generated [GraphQLDocumentRegistry].
         *
         * @param context A valid application context
         * @param registry A build-time generated registry of GraphQL operations
         * @param level Minimum log level
         */
        @JvmOverloads
        fun create(
            context: Context,
            registry: GraphQLDocumentRegistry,
            level: ILogger.Level = ILogger.Level.INFO,
        ): GraphConverter =
            GraphConverter(
                graphProcessor = assetBackedProcessor(context, level),
                json = defaultJson(),
                registry = registry,
            )

        /**
         * Creates a [GraphConverter] with a custom [Gson] configuration and a build-time
         * generated [GraphQLDocumentRegistry].
         *
         * @param context A valid application context
         * @param gson Custom gson implementation
         * @param registry A build-time generated registry of GraphQL operations
         * @param level Minimum log level
         */
        @JvmOverloads
        fun create(
            context: Context,
            gson: Gson,
            registry: GraphQLDocumentRegistry,
            level: ILogger.Level = ILogger.Level.INFO,
        ): GraphConverter =
            GraphConverter(
                graphProcessor = assetBackedProcessor(context, level),
                json = GsonGraphQLJson(gson),
                registry = registry,
            )

        /**
         * Creates a [GraphConverter] with a custom [GraphQLJson] instance and a build-time
         * generated [GraphQLDocumentRegistry].
         *
         * @param context A valid application context
         * @param json A [GraphQLJson] implementation for serialization
         * @param registry A build-time generated registry of GraphQL operations
         * @param level Minimum log level
         */
        @JvmOverloads
        fun create(
            context: Context,
            json: GraphQLJson,
            registry: GraphQLDocumentRegistry,
            level: ILogger.Level = ILogger.Level.INFO,
        ): GraphConverter =
            GraphConverter(
                graphProcessor = assetBackedProcessor(context, level),
                json = json,
                registry = registry,
            )

        // -------------------------------------------------------
        // Factory methods without Context (registry-only processor)
        // -------------------------------------------------------

        /**
         * Creates a [GraphConverter] that resolves operation documents from a
         * build-time generated [GraphQLDocumentRegistry] without requiring an Android [Context].
         *
         * If a requested [GraphQuery] operation is not registered, request conversion fails fast
         * with [IllegalStateException] because no asset fallback is available in this mode.
         *
         * @param registry A build-time generated registry of GraphQL operations.
         * @param level Minimum log level.
         */
        @JvmOverloads
        fun create(
            registry: GraphQLDocumentRegistry,
            level: ILogger.Level = ILogger.Level.INFO,
        ): GraphConverter =
            GraphConverter(
                graphProcessor = registryOnlyProcessor(level),
                json = defaultJson(),
                registry = registry,
            )

        /**
         * Creates a registry-first [GraphConverter] with a custom [Gson] instance and without
         * requiring an Android [Context].
         *
         * If a requested [GraphQuery] operation is not registered, request conversion fails fast
         * with [IllegalStateException] because no asset fallback is available in this mode.
         *
         * @param gson Custom gson implementation.
         * @param registry A build-time generated registry of GraphQL operations.
         * @param level Minimum log level.
         */
        @JvmOverloads
        fun create(
            gson: Gson,
            registry: GraphQLDocumentRegistry,
            level: ILogger.Level = ILogger.Level.INFO,
        ): GraphConverter =
            GraphConverter(
                graphProcessor = registryOnlyProcessor(level),
                json = GsonGraphQLJson(gson),
                registry = registry,
            )

        /**
         * Creates a registry-first [GraphConverter] with a custom [GraphQLJson] instance
         * and without requiring an Android [Context].
         *
         * If a requested [GraphQuery] operation is not registered, request conversion fails fast
         * with [IllegalStateException] because no asset fallback is available in this mode.
         *
         * @param json A [GraphQLJson] implementation for serialization.
         * @param registry A build-time generated registry of GraphQL operations.
         * @param level Minimum log level.
         */
        @JvmOverloads
        fun create(
            json: GraphQLJson,
            registry: GraphQLDocumentRegistry,
            level: ILogger.Level = ILogger.Level.INFO,
        ): GraphConverter =
            GraphConverter(
                graphProcessor = registryOnlyProcessor(level),
                json = json,
                registry = registry,
            )
    }
}
