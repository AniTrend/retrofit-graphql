/**
 * Copyright 2026 AniTrend
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

package co.anitrend.retrofit.graphql.model

import co.anitrend.retrofit.graphql.model.request.PersistedQuery
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A typed GraphQL request payload.
 *
 * **Legacy**: this serializer-coupled request type lives in `:compat` for
 * backward compatibility with the deprecated [co.anitrend.retrofit.graphql.converter.GraphConverter]
 * flow. New code should use the backend-neutral
 * [co.anitrend.retrofit.graphql.model.request.GraphQLOperationRequest] with
 * [co.anitrend.retrofit.graphql.converter.GraphQLConverterFactory].
 *
 * It is
 * annotated with [kotlinx.serialization.Serializable] to enable serialization
 * by [KotlinxGraphQLJson] when Retrofit passes a parameterized type like
 * `GraphQLRequest<GetMarketPlaceAppsVariables>`.
 *
 * When using the codegen Gradle plugin, generated operation objects provide
 * `.request(...)` factory methods that construct
 * [co.anitrend.retrofit.graphql.model.request.GraphQLOperationRequest]
 * instances with type-safe variable classes. For asset-based workflows, construct
 * [GraphQLRequest] manually or use [QueryContainerBuilder].
 *
 * ## Serialization Behavior
 *
 * ### kotlinx.serialization
 * - [query], [operationName], and [variables] are serialized normally
 * - [extensions] remains `@Transient` on the data class because
 *   `Map<String, Any?>` cannot be statically resolved by the compiler plugin
 * - [KotlinxGraphQLJson.encode] merges supported extension payloads into the
 *   encoded JSON at runtime so common request extensions, including APQ,
 *   continue to work on the typed request flow
 *
 * ### Gson (via GsonGraphQLJson)
 * - All fields are serialized normally, including [extensions]
 *
 * ### Accessing [extensions] with kotlinx
 *
 * [withPersistedQuery()] already works through [KotlinxGraphQLJson.encode]
 * without a custom serializer. For other extension values (e.g. custom
 * protocol extensions, vendor-specific metadata), define your own
 * `@Serializable` request wrapper with concrete JSON element types. No custom
 * serializer is required unless the actual wire structure needs transformation:
 * ```kotlin
 * @Serializable
 * data class JsonGraphQLRequest<TVariables : GraphQLVariables>(
 *     val query: String,
 *     val operationName: String,
 *     val variables: TVariables? = null,
 *     val extensions: JsonObject? = null,
 * )
 * ```
 * Use this wrapper only as the request body type for endpoints whose extension
 * payloads are already represented as JSON. Keep using [GraphQLRequest] when
 * [KotlinxGraphQLJson.encode] can encode the extension values directly.
 * [withPersistedQuery()] handles APQ without any custom serializer.
 * This example covers arbitrary extension payloads beyond APQ.
 *
 * ### APQ behavior
 * [withPersistedQuery()] still works with [KotlinxGraphQLJson] for the typed
 * [GraphQLRequest] flow. The runtime encoder merges `extensions.persistedQuery`
 * into the outgoing JSON even though [extensions] stays `@Transient` on the
 * data class. The legacy [QueryContainerBuilder] flow remains Gson-backed.
 *
 * ## Usage
 *
 * Manual (asset-based):
 * ```kotlin
 * val request = GraphQLRequest<EmptyGraphQLVariables>(
 *     query = "query GetCurrentUser { viewer { login } }",
 *     operationName = "GetCurrentUser",
 * )
 * ```
 *
 * @param TVariables The type of variables, or [EmptyGraphQLVariables] for operations without variables.
 * @property query The full GraphQL document string.
 * @property operationName The operation name.
 * @property variables The operation variables, or null if there are none.
 * @property extensions Optional extensions map (e.g. persistedQuery). Stored on
 *   the request object and serialized by Gson directly; [KotlinxGraphQLJson]
 *   merges supported entries into the outgoing JSON at encode time.
 * @see GraphQLVariables
 * @see EmptyGraphQLVariables
 * @see GraphQLDocumentRegistry
 * @see co.anitrend.retrofit.graphql.model.request.GraphQLOperationRequest
 */
@Deprecated(
    "Legacy serializer-coupled request payload. Use GraphQLOperationRequest with the backend-neutral GraphQLConverterFactory instead.",
)
@Serializable
data class GraphQLRequest<TVariables : GraphQLVariables>(
    val query: String,
    val operationName: String,
    val variables: TVariables? = null,
    @Transient
    val extensions: Map<String, Any?> = emptyMap(),
) {
    /**
     * Returns a copy of this request with a persisted query extension added.
     */
    fun withPersistedQuery(
        sha256Hash: String,
        version: Int = 1,
    ): GraphQLRequest<TVariables> =
        copy(
            extensions =
                extensions +
                    mapOf(
                        "persistedQuery" to
                            PersistedQuery(
                                sha256Hash = sha256Hash,
                                version = version,
                            ),
                    ),
        )
}
