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

package co.anitrend.retrofit.graphql.codegen.parser

import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.OperationType
import co.anitrend.retrofit.graphql.codegen.model.OutputField
import co.anitrend.retrofit.graphql.codegen.model.ResponseField
import co.anitrend.retrofit.graphql.codegen.model.ResponseSelectionSet
import co.anitrend.retrofit.graphql.codegen.model.RuntimeBranch
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import co.anitrend.retrofit.graphql.codegen.model.SchemaType
import co.anitrend.retrofit.graphql.codegen.model.SelectionCondition
import graphql.language.Field
import graphql.language.FragmentDefinition
import graphql.language.FragmentSpread
import graphql.language.InlineFragment
import graphql.language.OperationDefinition
import graphql.language.Selection
import graphql.language.TypeName
import graphql.language.VariableReference
import graphql.parser.Parser
import graphql.parser.ParserOptions

/**
 * Parses the selection set of a GraphQL operation against a schema
 * index and produces a normalized [ResponseSelectionSet] that
 * generators can consume to produce response model types.
 *
 * This is the compiler foundation: it resolves field names against
 * the schema, handles aliases, merges repeated compatible fields,
 * tracks directive-derived conditions, and produces deterministic
 * output independent of input file field ordering.
 *
 * @param schemaIndex The indexed schema metadata used to resolve
 *   fields and types.
 */
class ResponseSelectionParser(
    private val schemaIndex: SchemaIndex,
) {
    private companion object {
        /** Synthetic `__typename` field auto-injected for abstract type selections. */
        val TYPE_NAME_FIELD: ResponseField = ResponseField(
            responseName = "__typename",
            schemaName = "__typename",
            outputType = GraphQLType.Named("String", nullable = false),
            condition = SelectionCondition.UNCONDITIONAL,
            // applicableTypes empty = applies to all concrete types
        )

        /**
         * Creates [RuntimeBranch] instances for the given fragment scope,
         * expanding interface type conditions into their concrete implementors.
         *
         * @param abstractPath The response path to the abstract parent field.
         * @param typeName The fragment type condition name.
         * @param schemaIndex Used to resolve interface implementors.
         * @return One [RuntimeBranch] per concrete type in the scope.
         */
        fun buildRuntimeBranches(
            abstractPath: List<String>,
            typeName: String,
            schemaIndex: SchemaIndex,
        ): List<RuntimeBranch> {
            val concreteTypes =
                schemaIndex.possibleTypesFor(typeName)
                    .ifEmpty { setOf(typeName) }
            return concreteTypes.map { concreteType ->
                RuntimeBranch(
                    abstractPath = abstractPath,
                    concreteType = concreteType,
                )
            }
        }

        /**
         * Tests whether two lists of [RuntimeBranch] are compatible
         * (i.e. their concrete types at matching abstract paths could
         * overlap). Returns `true` if the branches do NOT conflict.
         */
        fun areBranchesCompatible(
            a: List<RuntimeBranch>,
            b: List<RuntimeBranch>,
        ): Boolean {
            // Empty branches mean "applies to all" --- compatible
            if (a.isEmpty() || b.isEmpty()) return true
            val aMap = a.associateBy { it.abstractPath }
            val bMap = b.associateBy { it.abstractPath }
            for ((path, aBranch) in aMap) {
                val bBranch = bMap[path] ?: return true
                if (aBranch.concreteType != bBranch.concreteType) return false
            }
            return true
        }
    }

    init {
        val options =
            ParserOptions.newParserOptions()
                .maxTokens(Int.MAX_VALUE)
                .build()
        ParserOptions.setDefaultParserOptions(options)
    }

    /**
     * Parses the top-level selection set of a GraphQL operation
     * against the schema root type and returns a normalized
     * [ResponseSelectionSet].
     *
     * @param operation The parsed operation info containing the
     *   document text and operation type.
     * @return A normalized response selection set rooted at the
     *   query, mutation, or subscription type.
     * @throws IllegalArgumentException if the root type is not
     *   found or a field cannot be resolved.
     */
    fun parse(operation: GraphQLOperationInfo): ResponseSelectionSet {
        val rootTypeName = resolveRootTypeName(operation.type)
        val rootObjectType =
            requireNotNull(schemaIndex.objectType(rootTypeName)) {
                "Root operation type '$rootTypeName' not found in schema."
            }

        val document = Parser().parseDocument(operation.document)
        val operationDef =
            requireNotNull(
                document.definitions
                    .filterIsInstance<OperationDefinition>()
                    .firstOrNull { it.name == operation.name },
            ) {
                "Operation '${operation.name}' not found in document."
            }

        val fragmentMap = buildFragmentMap(document)

        return parseSelectionSet(
            parentType = rootObjectType,
            selections = operationDef.selectionSet.selections,
            fragmentMap = fragmentMap,
        )
    }

    // --- Private helpers ---

    private fun resolveRootTypeName(type: OperationType): String {
        return when (type) {
            OperationType.QUERY ->
                schemaIndex.queryTypeName
                    ?: throw IllegalArgumentException(
                        "Schema has no query root type.",
                    )
            OperationType.MUTATION ->
                schemaIndex.mutationTypeName
                    ?: throw IllegalArgumentException(
                        "Schema has no mutation root type.",
                    )
            OperationType.SUBSCRIPTION ->
                schemaIndex.subscriptionTypeName
                    ?: throw IllegalArgumentException(
                        "Schema has no subscription root type.",
                    )
        }
    }

    private fun buildFragmentMap(
        document: graphql.language.Document,
    ): Map<String, FragmentDefinition> {
        return document.definitions
            .filterIsInstance<FragmentDefinition>()
            .associateBy { it.name }
    }

    /**
     * Parses a list of selections against a parent object type and
     * produces a normalized [ResponseSelectionSet].
     *
     * @param responseIdentity The path-based identity for this
     *   selection set, used to produce unique class names per
     *   response path. Empty for the root.
     */
    private fun parseSelectionSet(
        parentType: SchemaType,
        selections: List<Selection<*>>,
        fragmentMap: Map<String, FragmentDefinition>,
        responseIdentity: String = "",
    ): ResponseSelectionSet {
        val rawFields = mutableListOf<ResponseField>()

        for (selection in selections) {
            when (selection) {
                is Field -> {
                    val parsed = parseField(
                        field = selection,
                        parentType = parentType,
                        fragmentMap = fragmentMap,
                        parentIdentity = responseIdentity,
                    )
                    if (parsed != null) {
                        rawFields.add(parsed)
                    }
                }
                is FragmentSpread -> {
                    val fragment =
                        requireNotNull(fragmentMap[selection.name]) {
                            "Fragment '${selection.name}' not found."
                        }
                    val fragmentTypeName = fragment.typeCondition.name!!
                    val fragmentType =
                        resolveFragmentType(fragment.typeCondition)
                    val fragmentSelections =
                        fragment.selectionSet?.selections.orEmpty()

                    // Build abstract path from the parent selection-set identity
                    val abstractPath =
                        responseIdentity.ifEmpty { "" }
                            .split(".")
                            .filter { it.isNotEmpty() }
                    val branches =
                        buildRuntimeBranches(
                            abstractPath = abstractPath,
                            typeName = fragmentTypeName,
                            schemaIndex = schemaIndex,
                        )
                    val applicableTypes =
                        branches.map { it.concreteType }.toSet()

                    val dirCond = directivesToCondition(selection.directives)
                    // If the fragment's own directives exclude it
                    // (e.g. @include(if: false)), skip its fields.
                    val spreadFields =
                        if (dirCond == null) {
                            emptyList()
                        } else {
                            parseSelectionSet(
                                parentType = fragmentType,
                                selections = fragmentSelections,
                                fragmentMap = fragmentMap,
                                responseIdentity = responseIdentity,
                            ).fields.map { field ->
                                val newBranches =
                                    field.runtimeBranches + branches
                                field.copy(
                                    applicableTypes =
                                    composeApplicableTypes(
                                        field.computeApplicableTypes(),
                                        applicableTypes,
                                    ),
                                    runtimeBranches = newBranches,
                                    condition = mergeConditions(
                                        field.condition,
                                        dirCond,
                                    ),
                                ).propagateBranches()
                            }
                        }
                    rawFields.addAll(spreadFields)
                }
                is InlineFragment -> {
                    val inlineTypeName = selection.typeCondition?.name
                        ?: parentType.name
                    val inlineType =
                        if (selection.typeCondition != null) {
                            resolveFragmentType(selection.typeCondition)
                        } else {
                            parentType
                        }
                    val inlineSelections =
                        selection.selectionSet?.selections.orEmpty()

                    val abstractPath =
                        responseIdentity.ifEmpty { "" }
                            .split(".")
                            .filter { it.isNotEmpty() }
                    val branches =
                        if (selection.typeCondition != null) {
                            buildRuntimeBranches(
                                abstractPath = abstractPath,
                                typeName = inlineTypeName,
                                schemaIndex = schemaIndex,
                            )
                        } else {
                            emptyList()
                        }
                    val applicableTypes =
                        branches.map { it.concreteType }.toSet()

                    val dirCond = directivesToCondition(selection.directives)
                    // If the fragment's own directives exclude it
                    // (e.g. @include(if: false)), skip its fields.
                    val inlineFields =
                        if (dirCond == null) {
                            emptyList()
                        } else {
                            parseSelectionSet(
                                parentType = inlineType,
                                selections = inlineSelections,
                                fragmentMap = fragmentMap,
                                responseIdentity = responseIdentity,
                            ).fields.map { field ->
                                val newBranches =
                                    field.runtimeBranches + branches
                                field.copy(
                                    applicableTypes =
                                    composeApplicableTypes(
                                        field.computeApplicableTypes(),
                                        applicableTypes,
                                    ),
                                    runtimeBranches = newBranches,
                                    condition = mergeConditions(
                                        field.condition,
                                        dirCond,
                                    ),
                                ).propagateBranches()
                            }
                        }
                    rawFields.addAll(inlineFields)
                }
                else -> {
                    throw IllegalArgumentException(
                        "Unsupported selection type: ${selection::class.simpleName}",
                    )
                }
            }
        }

        // Merge repeated fields by responseName, then sort deterministically
        val mergedFields = mergeFields(rawFields).sortedBy { it.responseName }
        return ResponseSelectionSet(
            parentType = parentType.name,
            responseIdentity = responseIdentity,
            fields = mergedFields,
        )
    }

    /**
     * Parses a single field selection against a parent object type.
     *
     * @param parentIdentity The path-based identity of the parent
     *   selection set, used to build child identities.
     */
    private fun parseField(
        field: Field,
        parentType: SchemaType,
        fragmentMap: Map<String, FragmentDefinition>,
        parentIdentity: String = "",
    ): ResponseField? {
        val schemaName = field.name
        val responseName = field.alias ?: schemaName

        // __typename is a meta-field — don't look it up in the schema
        if (schemaName == "__typename") {
            return ResponseField(
                responseName = responseName,
                schemaName = schemaName,
                outputType = GraphQLType.Named("String", nullable = false),
            )
        }

        val fieldDef =
            requireNotNull(
                getOutputFields(parentType).find { it.name == schemaName },
            ) {
                "Field '$schemaName' not found on type '${parentType.name}'."
            }

        val condition = directivesToCondition(field.directives)
        // If the directive literal consistently excludes this field
        // (e.g. @include(if: false) or @skip(if: true)), skip it entirely.
        if (condition == null) return null

        // Resolve the field's return type to determine possible types
        val typeName = resolveNamedType(fieldDef.type)
        val def = schemaIndex.definition(typeName)

        val possibleTypes =
            when (def) {
                is SchemaType.InterfaceType,
                is SchemaType.UnionType,
                -> schemaIndex.possibleTypesFor(typeName)
                else -> emptySet()
            }

        // Compute response-path identity from parent identity + this field's response name.
        // Uses '.' as separator since it is not valid in GraphQL field names
        // (GraphQL names match /[_A-Za-z][_0-9A-Za-z]*/), preventing collisions
        // between flat field names and nested paths.
        val childIdentity =
            if (parentIdentity.isEmpty()) {
                responseName
            } else {
                "$parentIdentity.$responseName"
            }

        // Parse nested selections if present
        val childSelections = field.selectionSet?.selections.orEmpty()
        val nestedSet: ResponseSelectionSet? =
            if (childSelections.isNotEmpty()) {
                // Use the field's declared return type as the parent for nested
                // selections, not the first concrete implementor. This ensures
                // that interface/union fields are resolved against the abstract
                // type's own field list (which is the contract), rather than
                // accidentally using a subtype that may have narrower types.
                val childType = when (def) {
                    is SchemaType.ObjectType,
                    is SchemaType.InterfaceType,
                    is SchemaType.UnionType,
                    -> resolveFragmentType(
                        TypeName.newTypeName(typeName).build(),
                    )
                    else -> parentType
                }
                parseSelectionSet(
                    parentType = childType,
                    selections = childSelections,
                    fragmentMap = fragmentMap,
                    responseIdentity = childIdentity,
                )
            } else {
                null
            }

        // Auto-inject __typename for abstract types when there is a
        // selection set and __typename was not explicitly selected.
        // Only unaliased __typename satisfies the discriminator;
        // an aliased `kind: __typename` does not suppress injection.
        val hasTypename =
            nestedSet?.fields?.any {
                it.schemaName == "__typename" && it.responseName == "__typename"
            } ?: false
        val finalSet =
            if (
                !hasTypename &&
                (def is SchemaType.InterfaceType || def is SchemaType.UnionType) &&
                nestedSet != null
            ) {
                nestedSet.copy(
                    fields = listOf(TYPE_NAME_FIELD) + nestedSet.fields,
                )
            } else {
                nestedSet
            }

        return ResponseField(
            responseName = responseName,
            schemaName = schemaName,
            outputType = fieldDef.type,
            condition = condition,
            possibleTypes = possibleTypes,
            selectionSet = finalSet,
        )
    }

    /**
     * Merges fields that share the same [ResponseField.responseName].
     * Compatible fields (same schemaName, same top-level output type)
     * have their nested selection sets combined. Incompatible fields
     * from mutually exclusive runtime branches are stored as
     * [ResponseField.alternatives] rather than causing an error.
     */
    private fun mergeFields(
        fields: List<ResponseField>,
    ): List<ResponseField> {
        val grouped = linkedMapOf<String, MutableList<ResponseField>>()
        for (field in fields) {
            grouped.getOrPut(field.responseName) { mutableListOf() }
                .add(field)
        }

        return grouped.map { (responseName, group) ->
            if (group.size == 1) return@map group.first()

            // Multiple fields with same responseName -- must be compatible
            val first = group.first()
            // Resolve applicableTypes from branches if not already set
            val normalized = group.map { f ->
                if (f.applicableTypes.isEmpty() && f.runtimeBranches.isNotEmpty()) {
                    f.copy(applicableTypes = f.computeApplicableTypes())
                } else {
                    f
                }
            }

            // Check for schema name conflicts and handle via alternatives
            var result = normalized.first()
            var allAlternatives = result.alternatives.toMutableList()

            for (other in normalized.drop(1)) {
                val sameSchemaName = other.schemaName == result.schemaName
                val branchesCompatible =
                    areBranchesCompatible(
                        result.runtimeBranches,
                        other.runtimeBranches,
                    )

                if (sameSchemaName && branchesCompatible) {
                    // Compatible: merge the nested selection sets and
                    // accumulate applicableTypes (union of scopes).
                    val mergedApplicableTypes =
                        if (
                            result.applicableTypes.isEmpty() ||
                            other.applicableTypes.isEmpty()
                        ) {
                            emptySet<String>()
                        } else {
                            result.applicableTypes + other.applicableTypes
                        }
                    val mergedBranches =
                        result.runtimeBranches.toSet() +
                            other.runtimeBranches.toSet()

                    val mergedSelectionSet =
                        listOfNotNull(result.selectionSet, other.selectionSet)
                            .fold(null as ResponseSelectionSet?) { acc, set ->
                                if (acc == null) {
                                    set
                                } else {
                                    ResponseSelectionSet(
                                        parentType = acc.parentType,
                                        responseIdentity = acc.responseIdentity,
                                        fields = mergeFields(
                                            acc.fields + set.fields,
                                        ),
                                    )
                                }
                            }

                    result = result.copy(
                        selectionSet = mergedSelectionSet,
                        applicableTypes = mergedApplicableTypes,
                        runtimeBranches = mergedBranches.toList(),
                    )
                } else if (!sameSchemaName) {
                    // Incompatible schema names from mutually exclusive
                    // branches: keep as alternative.
                    allAlternatives.add(other)
                } else {
                    // Same schema name but incompatible branches:
                    // this shouldn't normally happen but if it does,
                    // merge the nested fields and keep branches.
                    val mergedSelectionSet =
                        listOfNotNull(result.selectionSet, other.selectionSet)
                            .fold(null as ResponseSelectionSet?) { acc, set ->
                                if (acc == null) {
                                    set
                                } else {
                                    ResponseSelectionSet(
                                        parentType = acc.parentType,
                                        responseIdentity = acc.responseIdentity,
                                        fields = mergeFields(
                                            acc.fields + set.fields,
                                        ),
                                    )
                                }
                            }

                    result = result.copy(
                        selectionSet = mergedSelectionSet,
                        applicableTypes =
                        result.applicableTypes + other.applicableTypes,
                        runtimeBranches =
                        result.runtimeBranches + other.runtimeBranches,
                    )
                }
            }

            result.copy(alternatives = allAlternatives)
        }
    }

    /**
     * Resolves a [TypeName] to a [SchemaType], handling fragment type
     * conditions that may target interfaces, unions, or concrete object types.
     *
     * For interface type conditions, returns the interface definition directly
     * (so fields can be resolved from the interface's own field list). For union
     * type conditions, returns the union definition. For object type conditions,
     * returns the object type directly.
     */
    private fun resolveFragmentType(
        typeName: TypeName?,
    ): SchemaType {
        val name =
            requireNotNull(typeName?.name) {
                "Type condition must have a name."
            }
        val def =
            requireNotNull(schemaIndex.definition(name)) {
                "Type '$name' not found in schema."
            }
        return when (def) {
            is SchemaType.ObjectType -> def
            is SchemaType.InterfaceType -> def
            is SchemaType.UnionType -> def
            else -> throw IllegalArgumentException(
                "Fragment type condition '$name' is not a composite type " +
                    "(found ${def::class.simpleName})",
            )
        }
    }

    /**
     * Gets the output fields defined on a schema type, handling
     * objects, interfaces, and unions uniformly. Objects and interfaces
     * return their field lists; unions and other types return empty.
     */
    private fun getOutputFields(type: SchemaType): List<OutputField> = when (type) {
        is SchemaType.ObjectType -> type.fields
        is SchemaType.InterfaceType -> type.fields
        else -> emptyList()
    }

    /**
     * Composes the [existing] applicable types with an [outerScope]
     * from a parent fragment. When both are non-empty, the result
     * is their intersection (narrowing the scope). When one is empty,
     * the other wins. This prevents outer fragments from over-expanding
     * the scope of nested fragments.
     */
    private fun composeApplicableTypes(
        existing: Set<String>,
        outerScope: Set<String>,
    ): Set<String> = when {
        existing.isEmpty() -> outerScope
        outerScope.isEmpty() -> existing
        else -> existing.intersect(outerScope)
    }

    /**
     * Propagates this field's [ResponseField.runtimeBranches] to its
     * children within its selection set, continuing to grandchildren
     * and deeper. For children that already have their own
     * runtimeBranches from a nested fragment scope, the parent's
     * branches are appended (accumulated) rather than overwriting.
     * This ensures that nested abstract type branches correctly
     * capture the full chain from outer to inner levels.
     *
     * Also populates applicableTypes from runtimeBranches for backward
     * compatibility.
     */
    private fun ResponseField.propagateBranches(): ResponseField {
        val branches = runtimeBranches
        if (branches.isEmpty() || selectionSet == null) return this
        val computedApplicable = branches.map { it.concreteType }.toSet()
        return copy(
            applicableTypes = computedApplicable,
            selectionSet = selectionSet.copy(
                fields = selectionSet.fields.map { child ->
                    if (child.runtimeBranches.isEmpty()) {
                        // Child has no scope yet -- apply parent
                        // branches and continue propagating deeper
                        child.copy(
                            runtimeBranches = branches,
                            applicableTypes = computedApplicable,
                        ).propagateBranches()
                    } else {
                        // Child already has scope from a nested
                        // fragment -- accumulate parent branches
                        // alongside the child's own branches, then
                        // continue propagating the merged set deeper
                        val mergedBranches =
                            branches +
                                child.runtimeBranches
                        val mergedApplicable =
                            mergedBranches
                                .map { it.concreteType }
                                .toSet()
                        child.copy(
                            runtimeBranches = mergedBranches,
                            applicableTypes = mergedApplicable,
                        ).propagateBranches()
                    }
                },
            ),
        )
    }

    /**
     * Extracts the innermost named type from a [GraphQLType], stripping
     * list and nullability wrappers.
     */
    private fun resolveNamedType(type: GraphQLType): String {
        return when (type) {
            is GraphQLType.Named -> type.name
            is GraphQLType.List -> resolveNamedType(type.of)
        }
    }

    /**
     * Converts a list of graphql-java [graphql.language.Directive] nodes
     * to a [SelectionCondition], handling `@include` and `@skip`.
     *
     * Literal boolean values are folded at parse time:
     * - `@include(if: true)` → unconditional (isConditional = false)
     * - `@include(if: false)` → returns null (field is always excluded)
     * - `@skip(if: false)` → unconditional (isConditional = false)
     * - `@skip(if: true)` → returns null (field is always excluded)
     */
    private fun directivesToCondition(
        directives: List<graphql.language.Directive>,
    ): SelectionCondition? {
        if (directives.isEmpty()) return SelectionCondition.UNCONDITIONAL

        for (directive in directives) {
            val arg =
                directive.getArgument("if")
                    ?: continue
            val value = arg.value

            when (directive.name) {
                "include" -> {
                    return when {
                        value is graphql.language.VariableReference -> {
                            SelectionCondition(
                                isConditional = true,
                                skipIf = false,
                                variableName = value.name,
                            )
                        }
                        value is graphql.language.BooleanValue &&
                            !value.isValue ->
                            // @include(if: false) → always excluded
                            null
                        else ->
                            // @include(if: true) or other literal → unconditional
                            SelectionCondition.UNCONDITIONAL
                    }
                }
                "skip" -> {
                    return when {
                        value is graphql.language.VariableReference -> {
                            SelectionCondition(
                                isConditional = true,
                                skipIf = true,
                                variableName = value.name,
                            )
                        }
                        value is graphql.language.BooleanValue &&
                            value.isValue ->
                            // @skip(if: true) → always excluded
                            null
                        else ->
                            // @skip(if: false) or other literal → unconditional
                            SelectionCondition.UNCONDITIONAL
                    }
                }
            }
        }

        return SelectionCondition.UNCONDITIONAL
    }

    /**
     * Merges two [SelectionCondition]s, combining their conditional
     * flags. If either condition is conditional, the merged result
     * is conditional.
     */
    private fun mergeConditions(
        base: SelectionCondition,
        override: SelectionCondition,
    ): SelectionCondition {
        if (!override.isConditional) return base
        if (!base.isConditional) return override
        // Both conditional: keep the base; fragment/inline conditions
        // propagate from the spread site, the field condition is the base
        return base
    }
}
