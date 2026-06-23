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

/**
 * A typed GraphQL request payload.
 *
 * When using the codegen Gradle plugin, generated operation objects provide
 * `.request(...)` factory methods that construct [GraphQLRequest] instances
 * with type-safe variable classes. For asset-based workflows, construct
 * [GraphQLRequest] manually or use [QueryContainerBuilder].
 *
 * Example (codegen):
 * ```kotlin
 * val request = GetMarketPlaceApps.request(first = 15, after = null)
 * ```
 *
 * Example (manual):
 * ```kotlin
 * val request = GraphQLRequest(
 *     query = "query GetCurrentUser { viewer { login } }",
 *     operationName = "GetCurrentUser",
 * )
 * ```
 *
 * @param TVariables The type of variables, or [EmptyGraphQLVariables] for operations without variables.
 * @property query The full GraphQL document string.
 * @property operationName The operation name.
 * @property variables The operation variables, or null if there are none.
 * @property extensions Optional extensions map (e.g. persistedQuery).
 * @see GraphQLVariables
 * @see GraphQLDocumentRegistry
 */
data class GraphQLRequest<TVariables : GraphQLVariables>(
    val query: String,
    val operationName: String,
    val variables: TVariables? = null,
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
