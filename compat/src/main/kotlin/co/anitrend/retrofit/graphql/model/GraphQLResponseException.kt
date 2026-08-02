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

import co.anitrend.retrofit.graphql.model.attribute.GraphError

/**
 * Thrown when a GraphQL response contains errors and the caller requires
 * non-null data.
 *
 * **Legacy**: part of the deprecated [GraphContainer] flow in `:compat`.
 *
 * @property errors The list of [GraphError] objects from the response.
 */
@Deprecated(
    "Legacy exception of the GraphContainer flow. " +
        "Use GraphQLResponseError handling with the backend-neutral GraphQLResponse contract instead.",
)
class GraphQLResponseException(
    val errors: List<GraphError>?,
) : RuntimeException(
        "GraphQL response contains errors: " +
            errors?.joinToString(separator = "; ", transform = GraphError::toString),
    )
