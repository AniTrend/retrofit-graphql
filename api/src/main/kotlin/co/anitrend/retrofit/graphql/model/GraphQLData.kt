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
 * Backend-neutral presence model for the `data` entry of a GraphQL response.
 *
 * [GraphQLData] distinguishes three wire states that a plain nullable field
 * would conflate:
 * - [Absent]: the response contains no `data` entry at all.
 * - [Present] with a non-null value: the response contains `data` with a value.
 * - [Present] with a null value: the response contains `data: null`.
 *
 * ## Usage
 *
 * ```kotlin
 * fun describe(data: GraphQLData<Viewer>) {
 *     when (data) {
 *         is GraphQLData.Absent -> println("no data entry")
 *         is GraphQLData.Present -> {
 *             val value: Viewer? = data.value
 *             println(if (value == null) "data: null" else "data present")
 *         }
 *     }
 * }
 * ```
 *
 * @param T The decoded type of the data entry.
 * @see GraphQLResponse
 */
sealed interface GraphQLData<out T> {
    /**
     * The response contains no `data` entry.
     *
     * This is a singleton, like the JSON `undefined` concept: the key is
     * missing from the payload rather than explicitly null.
     */
    data object Absent : GraphQLData<Nothing>

    /**
     * The response contains a `data` entry.
     *
     * @property value The decoded data, or null when the entry is `data: null`.
     */
    data class Present<out T>(
        val value: T?,
    ) : GraphQLData<T>
}
