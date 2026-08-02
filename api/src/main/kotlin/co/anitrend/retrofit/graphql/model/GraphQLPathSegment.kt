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
 * Backend-neutral segment of a GraphQL error [GraphQLResponseError.path].
 *
 * A GraphQL error path is a sequence of segments that locates the response
 * field which triggered the error. Each segment is either a named field
 * ([Field]) or a zero-based list index ([Index]).
 *
 * ## Usage
 *
 * ```kotlin
 * val path = listOf(
 *     GraphQLPathSegment.Field("viewer"),
 *     GraphQLPathSegment.Field("repositories"),
 *     GraphQLPathSegment.Index(3),
 * )
 * ```
 *
 * @see GraphQLResponseError
 */
sealed interface GraphQLPathSegment {
    /**
     * A named object field on the response path.
     *
     * @property name The field name as it appears in the GraphQL response.
     */
    data class Field(
        val name: String,
    ) : GraphQLPathSegment

    /**
     * A zero-based list index on the response path.
     *
     * @property value The zero-based position within a list field.
     */
    data class Index(
        val value: Int,
    ) : GraphQLPathSegment
}
