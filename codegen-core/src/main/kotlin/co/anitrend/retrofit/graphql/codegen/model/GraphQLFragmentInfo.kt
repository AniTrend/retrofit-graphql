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

package co.anitrend.retrofit.graphql.codegen.model

/**
 * Parsed information about a GraphQL fragment definition.
 *
 * @param name The fragment name, e.g. "UserFields".
 * @param document The source text of the fragment definition.
 * @param variableUsages Names of variables referenced within the fragment's
 *  selection set (e.g. `["format"]` if the fragment contains `age(format: $format)`).
 *  These are not variable *definitions* — fragments cannot define variables per
 *  the GraphQL spec — but rather variables the fragment *uses* that must be
 *  declared by the consuming operation.
 */
data class GraphQLFragmentInfo(
    val name: String,
    val document: String,
    val variableUsages: List<String> = emptyList(),
)
