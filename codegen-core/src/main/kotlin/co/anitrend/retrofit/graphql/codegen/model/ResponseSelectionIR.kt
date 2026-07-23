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
 * @param selectionSet Nested selections when this field returns an
 *   object, interface, or union type; null for leaf fields.
 */
data class ResponseField(
    val responseName: String,
    val schemaName: String,
    val outputType: GraphQLType,
    val condition: SelectionCondition = SelectionCondition.UNCONDITIONAL,
    val possibleTypes: Set<String> = emptySet(),
    val selectionSet: ResponseSelectionSet? = null,
)

/**
 * A normalized set of response fields selected from a single parent
 * type in the schema.
 *
 * @param parentType The schema type name that owns these fields.
 * @param fields The selected fields in deterministic order (sorted
 *   by [ResponseField.responseName]).
 */
data class ResponseSelectionSet(
    val parentType: String,
    val fields: List<ResponseField>,
) {
    companion object {
        /**
         * An empty selection set for a given [parentType].
         */
        fun empty(parentType: String): ResponseSelectionSet =
            ResponseSelectionSet(parentType = parentType, fields = emptyList())
    }
}
