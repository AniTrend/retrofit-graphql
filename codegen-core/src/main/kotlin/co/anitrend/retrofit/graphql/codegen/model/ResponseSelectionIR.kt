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
 * Represents a condition that controls whether a selection is included
 * in the response, derived from `@include` and `@skip` directives on
 * fields or fragment spreads.
 *
 * @param isConditional Whether this field is affected by any directive.
 * @param skipIf True for `@skip`, false for `@include`.
 * @param variableName The variable name used in the directive argument.
 */
data class SelectionCondition(
    val isConditional: Boolean = false,
    val skipIf: Boolean = false,
    val variableName: String? = null,
) {
    companion object {
        /** A non-conditional selection (no directives). */
        val UNCONDITIONAL: SelectionCondition = SelectionCondition()
    }
}

/**
 * Captures a single runtime type assignment for a nested abstract
 * hierarchy. Multiple branches represent the path through nested
 * interfaces/unions.
 *
 * @param abstractPath The response path to the abstract parent field,
 *   e.g. `["result"]` or `["outer", "inner"]`. Derived from the
 *   [ResponseSelectionSet.responseIdentity] split by `"."` at the
 *   fragment location.
 * @param concreteType The concrete runtime type at this level,
 *   e.g. `"Success"` or `"OuterA"`. When the fragment type condition
 *   is an interface, this is resolved to the actual concrete
 *   implementor type.
 */
data class RuntimeBranch(
    val abstractPath: List<String>,
    val concreteType: String,
)

/**
 * A single field in a response selection set, normalized against
 * the schema so generators can produce the correct Kotlin types.
 *
 * @param responseName The key in the JSON response (alias if present,
 *   otherwise the schema field name).
 * @param schemaName The actual field name on the schema type.
 * @param outputType The return type of this field as resolved from
 *   the schema.
 * @param condition The directive-derived condition controlling
 *   whether this field appears in the response.
 * @param possibleTypes When the parent type is an interface or union,
 *   the set of concrete object type names that are possible at runtime.
 *   Empty for concrete object fields.
 * @param applicableTypes **Deprecated** -- kept for backward
 *   compatibility. Populated from [runtimeBranches]. When non-empty,
 *   this field only belongs to the specified concrete types (used for
 *   inline fragment and fragment spread scoping on abstract
 *   selections). Empty means the field applies to all possible
 *   subtypes.
 * @param runtimeBranches Structured runtime branch paths capturing the
 *   chain through abstract hierarchies. Each entry records a concrete
 *   type assignment at a specific abstract-path level. Prefer this
 *   over [applicableTypes] for new code.
 * @param selectionSet Nested selections when this field returns an
 *   object, interface, or union type; null for leaf fields.
 * @param alternatives Field alternatives for mutually exclusive type
 *   scopes with incompatible schema names or output types. When the
 *   parser encounters two fields with the same responseName but
 *   different schema names (or incompatible output types) from
 *   disjoint concrete type branches, the alternatives are stored here
 *   rather than being merged or rejected.
 */
data class ResponseField(
    val responseName: String,
    val schemaName: String,
    val outputType: GraphQLType,
    val condition: SelectionCondition = SelectionCondition.UNCONDITIONAL,
    val possibleTypes: Set<String> = emptySet(),
    @Deprecated(
        message = "Use runtimeBranches instead",
        replaceWith = ReplaceWith("runtimeBranches"),
    )
    val applicableTypes: Set<String> = emptySet(),
    val runtimeBranches: List<RuntimeBranch> = emptyList(),
    val selectionSet: ResponseSelectionSet? = null,
    val alternatives: List<ResponseField> = emptyList(),
) {
    /**
     * Computes [applicableTypes] from [runtimeBranches] for backward
     * compatibility. Code that previously inspected [applicableTypes]
     * should migrate to [runtimeBranches] for correct nested branch
     * handling.
     */
    fun computeApplicableTypes(): Set<String> =
        runtimeBranches.map { it.concreteType }.toSet()
}

/**
 * A normalized set of response fields selected from a single parent
 * type in the schema.
 *
 * @param parentType The schema type name that owns these fields.
 * @param responseIdentity A path-based identifier for this selection
 *   set, derived from the response names along the path from the root.
 *   Used as the key for generating unique model classes per response
 *   path (e.g. "Viewer", "MediaEnglishTitle") instead of merging by
 *   schema type name. Empty for the top-level operation selection set.
 * @param fields The selected fields in deterministic order (sorted
 *   by [ResponseField.responseName]).
 */
data class ResponseSelectionSet(
    val parentType: String,
    val responseIdentity: String = "",
    val fields: List<ResponseField>,
) {
    companion object {
        /**
         * An empty selection set for a given [parentType].
         */
        fun empty(parentType: String): ResponseSelectionSet =
            ResponseSelectionSet(
                parentType = parentType,
                responseIdentity = "",
                fields = emptyList(),
            )
    }
}
