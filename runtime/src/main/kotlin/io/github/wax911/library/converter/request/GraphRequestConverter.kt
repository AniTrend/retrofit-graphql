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

package io.github.wax911.library.converter.request

import com.google.gson.Gson
import io.github.wax911.library.annotation.GraphQuery
import io.github.wax911.library.annotation.processor.contract.AbstractGraphProcessor
import io.github.wax911.library.model.GraphQLDocumentRegistry
import io.github.wax911.library.model.request.QueryContainerBuilder
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Converter

/**
 * GraphQL request body converter and injector, uses method annotation for a given retrofit method.
 *
 * Supports an optional [GraphQLDocumentRegistry] for build-time generated operation documents.
 * When a registry is provided, it is checked before falling back to the asset-based
 * [AbstractGraphProcessor] lookup.
 *
 * @param methodAnnotations Annotations applied to the Retrofit method.
 * @param graphProcessor The processor used for asset-based query resolution.
 * @param gson Gson instance for serialization.
 * @param registry Optional registry providing build-time generated documents and hashes.
 */
open class GraphRequestConverter(
    protected val methodAnnotations: Array<out Annotation>,
    protected val graphProcessor: AbstractGraphProcessor,
    protected val gson: Gson,
    protected val registry: GraphQLDocumentRegistry? = null,
) : Converter<QueryContainerBuilder, RequestBody> {
    /**
     * Converter for the request body, gets the GraphQL query from the method annotation
     * and constructs a GraphQL request body to send over the network.
     *
     * Resolution order:
     * 1. Build-time generated registry (if available and operation is registered)
     * 2. Asset-based file discovery via [AbstractGraphProcessor.getQuery]
     *
     * @param containerBuilder The constructed builder method of your query with variables
     */
    override fun convert(containerBuilder: QueryContainerBuilder): RequestBody {
        val rawQuery = resolveQuery()
        val queryContainer =
            containerBuilder.setQuery(rawQuery)
                .build()
        val queryJson = gson.toJson(queryContainer)
        return queryJson.toRequestBody(MEDIA_TYPE)
    }

    /**
     * Resolves the GraphQL query string using the following order:
     * 1. Build-time generated registry (if available)
     * 2. Asset-based file discovery
     */
    private fun resolveQuery(): String? {
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
        return methodAnnotations
            .filterIsInstance<GraphQuery>()
            .firstOrNull()
            ?.value
            ?.takeIf { it.isNotEmpty() }
    }

    companion object {
        private val MEDIA_TYPE = "application/json".toMediaTypeOrNull()
    }
}
