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

package co.anitrend.retrofit.graphql.data.arch.koin

import co.anitrend.retrofit.graphql.model.GraphQLDocumentRegistry

/**
 * Composes multiple [GraphQLDocumentRegistry] instances into a single registry.
 *
 * Delegates to each registry in order; returns the first non-null result.
 * This allows the bucket-specific operations (which use a separate schema)
 * to coexist with the codegen-generated GitHub API registry.
 */
internal class CompositeGraphQLRegistry(
    vararg delegates: GraphQLDocumentRegistry
) : GraphQLDocumentRegistry {
    private val registries: List<GraphQLDocumentRegistry> = delegates.toList()

    override fun document(operationName: String): String? =
        registries.firstNotNullOfOrNull { it.document(operationName) }

    override fun hash(operationName: String): String? =
        registries.firstNotNullOfOrNull { it.hash(operationName) }
}
