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

/**
 * Backend-neutral, spec-compliant GraphQL response envelope.
 *
 * [GraphQLResponse] is the transport contract for a single GraphQL response
 * before any Retrofit or HTTP concerns are applied. It is the neutral
 * counterpart of the legacy, serializer-coupled [co.anitrend.retrofit.graphql.model.body.GraphContainer].
 *
 * The response `data` entry is modeled with [GraphQLData] so that callers can
 * distinguish a missing `data` key ([GraphQLData.Absent]) from an explicit
 * `data: null` ([GraphQLData.Present] with a null value).
 *
 * ## Usage
 *
 * ```kotlin
 * val response = GraphQLResponse<Viewer>(
 *     data = GraphQLData.Present(viewer),
 *     extensions = GraphQLValue.ObjectValue(
 *         fields = mapOf(
 *             "cost" to GraphQLValue.ObjectValue(
 *                 fields = mapOf("requested" to GraphQLValue.NumberValue(BigDecimal(3))),
 *             ),
 *         ),
 *     ),
 * )
 * ```
 *
 * @param T The decoded type of the `data` entry.
 * @property data The `data` entry, or [GraphQLData.Absent] when the response omits it.
 * @property errors The GraphQL errors, or an empty list when the response is successful.
 * @property extensions Top-level response extensions (for example rate limit
 *   or cost metadata), or null when absent.
 *
 * @see [GraphQL Data Specification](http://spec.graphql.org/June2018/#sec-Data)
 * @see GraphQLData
 * @see GraphQLResponseError
 * @see GraphQLValue
 */
data class GraphQLResponse<T>(
    val data: GraphQLData<T> = GraphQLData.Absent,
    val errors: List<GraphQLResponseError> = emptyList(),
    val extensions: GraphQLValue.ObjectValue? = null,
)

/**
 * Backend-neutral, spec-compliant GraphQL error entry.
 *
 * [GraphQLResponseError] is the neutral counterpart of the legacy
 * [co.anitrend.retrofit.graphql.model.attribute.GraphError]. Unlike the
 * legacy type, [message] is required and non-null because a spec-compliant
 * error always carries a message.
 *
 * ## Usage
 *
 * ```kotlin
 * val error = GraphQLResponseError(
 *     message = "Cannot query field 'logins' on type 'User'.",
 *     locations = listOf(GraphQLResponseError.Location(line = 1, column = 10)),
 *     path = listOf(GraphQLPathSegment.Field("viewer")),
 * )
 * ```
 *
 * @property message Required description of the error.
 * @property locations Optional source locations in the GraphQL document.
 * @property path Optional response path segments that triggered the error.
 * @property extensions Optional additional error metadata represented by a
 *   neutral JSON object.
 *
 * @see [GraphQL Error Specification](http://spec.graphql.org/June2018/#sec-Errors)
 * @see GraphQLResponse
 * @see GraphQLPathSegment
 * @see GraphQLValue
 */
data class GraphQLResponseError(
    val message: String,
    val locations: List<Location>? = null,
    val path: List<GraphQLPathSegment>? = null,
    val extensions: GraphQLValue.ObjectValue? = null,
) {
    /**
     * Source location of an error within the GraphQL document.
     *
     * @property line The one-based line number.
     * @property column The one-based column number.
     */
    data class Location(
        val line: Int,
        val column: Int,
    )
}
