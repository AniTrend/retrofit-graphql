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

package co.anitrend.retrofit.graphql.model.body

import co.anitrend.retrofit.graphql.model.attribute.GraphError
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * GraphQL response that is spec compliant.
 *
 * This is the top-level response wrapper used as the Retrofit return type
 * for GraphQL operations. It is annotated with [kotlinx.serialization.Serializable]
 * to enable direct deserialization by [KotlinxGraphQLJson] when Retrofit
 * passes a parameterized type like `GraphContainer<GetCurrentUserData>`.
 *
 * ## Serialization Behavior
 *
 * ### kotlinx.serialization
 * - [data] is deserialized using the type parameter `T` (requires `@Serializable` on `T`)
 * - [errors] is deserialized via [GraphError] which is `@Serializable`
 * - [extensions] is `@Transient` (excluded) because `Map<Any, Any>` cannot be
 *   statically resolved by the kotlinx compiler plugin
 *
 * ### Gson (via GsonGraphQLJson)
 * - All fields are serialized/deserialized normally, including [extensions]
 *
 * ### Accessing [extensions] with kotlinx
 *
 * To read [extensions] while using kotlinx serialization, define your own
 * `@Serializable` response wrapper with concrete JSON element types. No custom
 * serializer is required unless the actual wire structure needs transformation:
 * ```kotlin
 * @Serializable
 * data class JsonGraphContainer<T>(
 *     val data: T? = null,
 *     val errors: List<JsonGraphError>? = null,
 *     val extensions: JsonObject? = null,
 * )
 *
 * @Serializable
 * data class JsonGraphError(
 *     val message: String? = null,
 *     val path: List<JsonElement>? = null,
 *     val locations: List<GraphError.Location>? = null,
 *     val extensions: JsonObject? = null,
 * )
 *
 * @POST("graphql")
 * suspend fun getCurrentUser(
 *     @Body request: GraphQLRequest<EmptyGraphQLVariables>
 * ): Response<JsonGraphContainer<GetCurrentUserData>>
 * ```
 * Alternatively, extract extension values from the raw JSON payload before
 * kotlinx deserialization runs.
 *
 * ## Usage with generated response DTOs
 *
 * ```kotlin
 * @POST("graphql")
 * suspend fun getCurrentUser(
 *     @Body request: GraphQLRequest<EmptyGraphQLVariables>
 * ): Response<GraphContainer<GetCurrentUserData>>
 * ```
 *
 * @param data The successful response data, or null if the response contains only errors.
 * @param errors A list of GraphQL errors, or null if the response is successful.
 *   Each error is deserialized via [GraphError].
 * @param extensions The response extensions map. Gson-only; `@Transient` for kotlinx.
 *
 * @see [GraphQL Data Specification](http://spec.graphql.org/June2018/#sec-Data)
 * @see GraphError
 * @see GraphQLRequest
 */
@Serializable
data class GraphContainer<T>(
    val data: T? = null,
    val errors: List<GraphError>? = null,
    @Transient
    val extensions: Map<Any, Any>? = null,
)
