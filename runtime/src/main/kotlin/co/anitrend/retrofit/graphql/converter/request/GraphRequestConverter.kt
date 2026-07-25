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

package co.anitrend.retrofit.graphql.converter.request

import co.anitrend.retrofit.graphql.annotation.GraphQuery
import co.anitrend.retrofit.graphql.annotation.processor.contract.AbstractGraphProcessor
import co.anitrend.retrofit.graphql.model.GraphQLDocumentRegistry
import co.anitrend.retrofit.graphql.model.GraphQLJson
import co.anitrend.retrofit.graphql.model.GraphQLRequest
import co.anitrend.retrofit.graphql.model.request.QueryContainerBuilder
import co.anitrend.retrofit.graphql.serialization.gson.GsonGraphQLJson
import com.google.gson.GsonBuilder
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Converter
import java.lang.reflect.Type

/**
 * GraphQL request body converter and injector, uses method annotation for a given retrofit method.
 *
 * Supports both the typed [GraphQLRequest] flow (recommended for codegen consumers)
 * and the legacy [QueryContainerBuilder] flow (Gson-only).
 *
 * ## Serialization
 *
 * Request serialization is delegated to [GraphQLJson.encode]:
 *
 * - **[GraphQLRequest]**: The request object is serialized directly. When [type]
 *   is available (provided by Retrofit), it is forwarded to [GraphQLJson.encode]
 *   so that serializers with parameterized type awareness (e.g. kotlinx.serialization)
 *   can resolve the correct `KSerializer<GraphQLRequest<FooVariables>>`.
 *
 * - **[QueryContainerBuilder]**: The built [QueryContainer] is serialized
 *   through Gson for backward compatibility. [QueryContainer] is not
 *   `@Serializable`, so the legacy builder flow always uses a Gson-backed
 *   encoder even when the response/request backend is kotlinx.serialization.
 *
 * ## Document Resolution
 *
 * When a [GraphQLDocumentRegistry] is provided, it is checked before falling back
 * to the asset-based [AbstractGraphProcessor] lookup (only for [QueryContainerBuilder] flow).
 * Registry-only converter factory overloads use a strict no-op processor that throws
 * when an annotated operation is missing.
 *
 * @param methodAnnotations Annotations applied to the Retrofit method.
 * @param graphProcessor The processor used for asset-based query resolution.
 * @param json Pluggable [GraphQLJson] instance for request body serialization.
 * @param registry Optional registry providing build-time generated documents and hashes.
 * @param type The type of the request body parameter, including parameterized type info
 *   (e.g. [GraphQLRequest]<FooVariables>). May be null when constructed outside Retrofit.
 */
open class GraphRequestConverter(
    protected val methodAnnotations: Array<out Annotation>,
    protected val graphProcessor: AbstractGraphProcessor,
    protected val json: GraphQLJson,
    protected val registry: GraphQLDocumentRegistry? = null,
    protected val type: Type? = null,
) : Converter<Any, RequestBody> {
    private val legacyJson: GraphQLJson by lazy {
        GsonGraphQLJson(
            GsonBuilder()
                .enableComplexMapKeySerialization()
                .serializeNulls()
                .setLenient()
                .create(),
        )
    }

    /**
     * Converter for the request body. Dispatches to the appropriate handler
     * based on the body type.
     *
     * @param value The request body object ([QueryContainerBuilder] or [GraphQLRequest]).
     */
    override fun convert(value: Any): RequestBody {
        return when (value) {
            is GraphQLRequest<*> -> convertGraphQLRequest(value)
            is QueryContainerBuilder -> convertQueryContainerBuilder(value)
            else -> throw IllegalArgumentException(
                "Unsupported request body type: ${value.javaClass.name}. " +
                    "Expected QueryContainerBuilder or GraphQLRequest.",
            )
        }
    }

    /**
     * Converts a [GraphQLRequest] to a JSON request body.
     * The request already contains the document, so no lookup is performed.
     *
     * When [type] is available (provided by Retrofit), it is forwarded to
     * [GraphQLJson.encode] so that serializers with parameterized type awareness
     * (e.g. kotlinx.serialization) can resolve the correct variable type.
     */
    private fun convertGraphQLRequest(request: GraphQLRequest<*>): RequestBody {
        val requestJson =
            if (type != null) json.encode(request, type) else json.encode(request)
        return requestJson.toRequestBody(MEDIA_TYPE)
    }

    /**
     * Converts a [QueryContainerBuilder] to a JSON request body.
     *
     * Resolution order:
     * 1. Build-time generated registry (if available and operation is registered)
     * 2. Asset-based file discovery via [AbstractGraphProcessor.getQuery]
     *
     * @param containerBuilder The constructed builder method of your query with variables
     */
    private fun convertQueryContainerBuilder(containerBuilder: QueryContainerBuilder): RequestBody {
        val rawQuery = resolveQuery()
        val queryContainer =
            containerBuilder.setQuery(rawQuery)
                .build()
        val queryJson =
            if (json is GsonGraphQLJson) {
                json.encode(queryContainer)
            } else {
                legacyJson.encode(queryContainer)
            }
        return queryJson.toRequestBody(MEDIA_TYPE)
    }

    /**
     * Resolves the GraphQL query string using the following order:
     * 1. Build-time generated registry (if available)
     * 2. Asset-based file discovery
     *
     * Returns the resolved document when one is found.
     * Returns `null` when the Retrofit method has no [GraphQuery] annotation and no lookup occurs.
     * Throws [IllegalStateException] in registry-only mode when an annotated operation is missing
     * from the registry and no asset fallback is available.
     */
    protected open fun resolveQuery(): String? {
        val operationName = extractOperationName()

        // Try the generated registry first
        if (operationName != null && registry != null) {
            val document = registry.document(operationName)
            if (document != null) return document
        }

        // Fall back to asset-based discovery
        return graphProcessor.getQuery(methodAnnotations)
    }

    /**
     * Extracts the operation name from the [GraphQuery] annotation value.
     */
    private fun extractOperationName(): String? {
        // Keep this extraction logic in sync with GraphConverter.registryOnlyProcessor().getQuery().
        val annotation =
            methodAnnotations.filterIsInstance<GraphQuery>().firstOrNull()
                ?: return null
        return annotation.value.takeIf { it.isNotEmpty() }
    }

    companion object {
        private val MEDIA_TYPE = "application/json".toMediaTypeOrNull()
    }
}
