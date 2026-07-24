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
 * A response path is a list of field response names from the root
 * of the query down to a particular selection.
 */
typealias ResponsePath = List<String>

/**
 * Represents a condition that controls whether a selection is included
 * in the response, derived from `@include` and `@skip` directives on
 * fields or fragment spreads.
 *
 * @param mayBeAbsent When true, the selection may be absent from the
 *   response (conditionally included or skipped). When false, the
 *   selection is unconditionally present.
 */
data class SelectionCondition(
    val mayBeAbsent: Boolean = false,
) {
    companion object {
        /** A non-conditional selection (no directives). */
        val UNCONDITIONAL: SelectionCondition = SelectionCondition()
    }
}

/**
 * A single runtime type assignment mapping an abstract path to its
 * concrete type. Used as a building block for [RuntimePath].
 */
data class RuntimeTypeAssignment(
    val abstractPath: ResponsePath,
    val concreteType: String,
)

/**
 * A conjunctive set of runtime type assignments. All assignments in
 * the map must be satisfied simultaneously.
 *
 * Example:
 * ```kotlin
 * RuntimePath(
 *     assignments = mapOf(
 *         listOf("outer") to "OuterA",
 *         listOf("outer", "inner") to "InnerX",
 *     ),
 * )
 * ```
 * means: `outer is OuterA AND outer.inner is InnerX`
 */
data class RuntimePath(
    val assignments: Map<ResponsePath, String>,
) {
    companion object {
        /** An empty runtime path (no type constraints). */
        val EMPTY: RuntimePath = RuntimePath(emptyMap())
    }
}

/**
 * A single field variant in a response selection set, normalized against
 * the schema so generators can produce correct Kotlin types.
 *
 * Fields are preserved as variants rather than merged prematurely.
 * Variants with the same [responseName] but different [schemaName]
 * or incompatible [runtimePaths] are kept as separate entries.
 *
 * @param responseName The key in the JSON response (alias if present,
 *   otherwise the schema field name).
 * @param schemaName The actual field name on the schema type.
 * @param outputType The return type of this field as resolved from
 *   the schema.
 * @param argumentsIdentity A normalized string representation of the
 *   field's arguments, or empty if there are no arguments.
 * @param condition The directive-derived condition controlling
 *   whether this field appears in the response.
 * @param selectionSet Nested selections when this field returns an
 *   object, interface, or union type; null for leaf fields.
 * @param runtimePaths The set of [RuntimePath] values that control
 *   when this variant is active. A field with [runtimePaths] empty
 *   is unconditional within its current selection set.
 */
data class ResponseFieldVariant(
    val responseName: String,
    val schemaName: String,
    val outputType: GraphQLType,
    val argumentsIdentity: String = "",
    val condition: SelectionCondition = SelectionCondition.UNCONDITIONAL,
    val selectionSet: ResponseSelectionSet? = null,
    val runtimePaths: Set<RuntimePath> = emptySet(),
)

/**
 * A normalized set of response field variants selected from a single
 * parent type in the schema.
 *
 * @param parentType The schema type name that owns these fields.
 * @param responsePath The response path from the root of the query
 *   to this selection set. Empty for the top-level operation.
 * @param fields The selected field variants in deterministic order
 *   (sorted by [ResponseFieldVariant.responseName]).
 */
data class ResponseSelectionSet(
    val parentType: String,
    val responsePath: ResponsePath = emptyList(),
    val fields: List<ResponseFieldVariant>,
) {
    companion object {
        /**
         * An empty selection set for a given [parentType].
         */
        fun empty(parentType: String): ResponseSelectionSet =
            ResponseSelectionSet(
                parentType = parentType,
                responsePath = emptyList(),
                fields = emptyList(),
            )
    }
}

/**
 * A single field after projection for a specific [RuntimePath].
 *
 * @param responseName The key in the JSON response.
 * @param schemaName The actual field name on the schema type.
 * @param outputType The return type of this field.
 * @param condition The directive-derived condition.
 * @param selectionSet Recursively projected nested selections, or null.
 */
data class ProjectedField(
    val responseName: String,
    val schemaName: String,
    val outputType: GraphQLType,
    val condition: SelectionCondition,
    val selectionSet: ProjectedSelectionSet? = null,
)

/**
 * A projected selection set for a specific [RuntimePath].
 *
 * @param parentType The schema type name that owns these fields.
 * @param responsePath The response path from the query root.
 * @param runtimePath The concrete runtime path that this projection
 *   was evaluated against.
 * @param fields The projected fields (exactly one per response name).
 */
data class ProjectedSelectionSet(
    val parentType: String,
    val responsePath: ResponsePath,
    val runtimePath: RuntimePath,
    val fields: List<ProjectedField>,
)

/**
 * The identity of a generated response model class.
 *
 * Used as the key for collecting models, deduplicating models,
 * assigning Kotlin class names, and resolving property types.
 *
 * @param responsePath The response path from the query root to this
 *   model's location in the JSON response.
 * @param runtimePath The runtime type constraints relevant to this
 *   model. Contains only assignments whose paths are prefixes of
 *   [responsePath].
 */
data class ResponseModelIdentity(
    val responsePath: ResponsePath,
    val runtimePath: RuntimePath,
)
