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

package co.anitrend.retrofit.graphql.model.request

import co.anitrend.retrofit.graphql.model.GraphQLValue
import co.anitrend.retrofit.graphql.model.GraphQLVariables
import java.math.BigDecimal

/**
 * Backend-neutral GraphQL operation request payload.
 *
 * [GraphQLOperationRequest] is the transport contract for a single GraphQL
 * request before any Retrofit or HTTP concerns are applied. It is the
 * neutral counterpart of the serializer-coupled [co.anitrend.retrofit.graphql.model.GraphQLRequest].
 *
 * All properties are immutable [val]s; use [copy] or [withPersistedQuery] to
 * derive a new request. [variables] is nullable because an operation may take
 * no variables.
 *
 * ## Usage
 *
 * ```kotlin
 * val request = GraphQLOperationRequest<EmptyGraphQLVariables>(
 *     query = "query GetCurrentUser { viewer { login } }",
 *     operationName = "GetCurrentUser",
 * )
 * ```
 *
 * @param TVariables The type of variables, or [co.anitrend.retrofit.graphql.model.EmptyGraphQLVariables] for operations without variables.
 * @property query The full GraphQL document string.
 * @property operationName The operation name.
 * @property variables The operation variables, or null when there are none.
 * @property extensions Optional protocol extensions (for example
 *   `persistedQuery`), or null when absent.
 *
 * @see GraphQLVariables
 * @see GraphQLValue
 */
data class GraphQLOperationRequest<TVariables : GraphQLVariables>(
    val query: String,
    val operationName: String,
    val variables: TVariables? = null,
    val extensions: GraphQLValue.ObjectValue? = null,
) {
    /**
     * Returns a copy of this request with a persisted query extension added.
     *
     * The extension follows the Apollo persisted query protocol:
     * `extensions.persistedQuery = { version, sha256Hash }`. Adding the
     * extension never mutates this instance; the original request keeps its
     * [extensions] unchanged.
     *
     * @param sha256Hash The SHA-256 hex digest of the GraphQL document.
     * @param version The protocol version, defaulting to `1`.
     * @return A new request whose [extensions] contain the persisted query entry.
     */
    fun withPersistedQuery(
        sha256Hash: String,
        version: Int = 1,
    ): GraphQLOperationRequest<TVariables> =
        copy(
            extensions =
                GraphQLValue.ObjectValue(
                    fields =
                        extensions?.fields.orEmpty() +
                            (
                                "persistedQuery" to
                                    GraphQLValue.ObjectValue(
                                        fields =
                                            mapOf(
                                                "sha256Hash" to GraphQLValue.StringValue(sha256Hash),
                                                "version" to GraphQLValue.NumberValue(BigDecimal(version)),
                                            ),
                                    )
                            ),
                ),
        )
}
