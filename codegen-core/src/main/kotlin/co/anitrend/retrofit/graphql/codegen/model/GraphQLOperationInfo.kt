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
 * Parsed information about a single GraphQL operation (query, mutation, or subscription).
 *
 * @param name The operation name, e.g. "GetCurrentUser".
 * @param type Whether this is a query, mutation, or subscription.
 * @param document The full source text of this operation, with fragments inlined.
 * @param sourceFile The file path from which this operation was parsed.
 * @param variables The variable definitions for this operation, if any.
 */
data class GraphQLOperationInfo(
    val name: String,
    val type: OperationType,
    val document: String,
    val sourceFile: String,
    val variables: List<GraphQLVariableInfo> = emptyList(),
)
