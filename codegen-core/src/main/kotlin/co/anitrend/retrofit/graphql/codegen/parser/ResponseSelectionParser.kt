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
import co.anitrend.retrofit.graphql.codegen.model.ResponseFieldVariant
import co.anitrend.retrofit.graphql.codegen.model.ResponsePath
import co.anitrend.retrofit.graphql.codegen.model.ResponseSelectionSet
import co.anitrend.retrofit.graphql.codegen.model.RuntimePath
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
import graphql.parser.Parser
import graphql.parser.ParserOptions

/**
 * Parses the selection set of a GraphQL operation against a schema
 * index and produces a normalized [ResponseSelectionSet] that
 * generators can consume to produce response model types.
 *
 * Fields are preserved as [ResponseFieldVariant] values. Variants are
 * merged only when all compatibility conditions are met AND both have
 * non-empty runtime paths (to preserve "applies to all" semantics).
 */
class ResponseSelectionParser(
    private val schemaIndex: SchemaIndex,
) {
    internal companion object {
        /** Synthetic `__typename` field auto-injected for abstract type selections. */
        val TYPE_NAME_FIELD: ResponseFieldVariant = ResponseFieldVariant(
            responseName = "__typename",
            schemaName = "__typename",
            outputType = GraphQLType.Named("String", nullable = false),
            condition = SelectionCondition.UNCONDITIONAL,
        )

        /**
         * Creates a set of [RuntimePath] values for a type-conditioned
         * fragment at the given response path, expanding interface type
         * conditions into their concrete implementors.
         */
        fun fragmentRuntimePaths(
            responsePath: ResponsePath,
            typeName: String,
            schemaIndex: SchemaIndex,
        ): Set<RuntimePath> {
            val concreteTypes =
                schemaIndex.possibleTypesFor(typeName)
                    .ifEmpty { setOf(typeName) }
            return concreteTypes.map { concreteType ->
                RuntimePath(
                    assignments = mapOf(responsePath to concreteType),
                )
            }.toSet()
        }

        /**
         * Composes enclosing and fragment runtime paths using Cartesian
         * product with conflict detection. Conflicting pairs (same path,
         * different types) are discarded.
         */
        fun composeRuntimePaths(
            enclosing: Set<RuntimePath>,
            fragment: Set<RuntimePath>,
        ): Set<RuntimePath> {
            if (enclosing.isEmpty()) return fragment
            if (fragment.isEmpty()) return enclosing

            return buildSet {
                for (left in enclosing) {
                    for (right in fragment) {
                        val conflict = left.assignments.any { (path, type) ->
                            val rightType = right.assignments[path]
                            rightType != null && rightType != type
                        }
                        if (!conflict) {
                            add(
                                RuntimePath(
                                    assignments = left.assignments + right.assignments,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    init {
        val options =
            ParserOptions.newParserOptions()
                .maxTokens(Int.MAX_VALUE)
                .build()
        ParserOptions.setDefaultParserOptions(options)
    }

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
            responsePath = emptyList(),
        )
    }

    private fun resolveRootTypeName(type: OperationType): String {
        return when (type) {
            OperationType.QUERY ->
                schemaIndex.queryTypeName
                    ?: throw IllegalArgumentException("Schema has no query root type.")
            OperationType.MUTATION ->
                schemaIndex.mutationTypeName
                    ?: throw IllegalArgumentException("Schema has no mutation root type.")
            OperationType.SUBSCRIPTION ->
                schemaIndex.subscriptionTypeName
                    ?: throw IllegalArgumentException("Schema has no subscription root type.")
        }
    }

    private fun buildFragmentMap(
        document: graphql.language.Document,
    ): Map<String, FragmentDefinition> {
        return document.definitions
            .filterIsInstance<FragmentDefinition>()
            .associateBy { it.name }
    }

    private fun parseSelectionSet(
        parentType: SchemaType,
        selections: List<Selection<*>>,
        fragmentMap: Map<String, FragmentDefinition>,
        responsePath: ResponsePath,
        enclosingRuntimePaths: Set<RuntimePath> = emptySet(),
    ): ResponseSelectionSet {
        val rawFields = mutableListOf<ResponseFieldVariant>()

        for (selection in selections) {
            when (selection) {
                is Field -> {
                    val parsed = parseField(
                        field = selection,
                        parentType = parentType,
                        fragmentMap = fragmentMap,
                        parentResponsePath = responsePath,
                        enclosingRuntimePaths = enclosingRuntimePaths,
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
                    val fragmentType = resolveFragmentType(fragment.typeCondition)
                    val fragmentSelections =
                        fragment.selectionSet.selections

                    val dirCond = directivesToCondition(selection.directives)
                    if (dirCond == null) continue

                    val fragPaths = fragmentRuntimePaths(
                        responsePath = responsePath,
                        typeName = fragmentTypeName,
                        schemaIndex = schemaIndex,
                    )
                    val composedPaths = composeRuntimePaths(
                        enclosingRuntimePaths,
                        fragPaths,
                    )

                    val spreadFields =
                        parseSelectionSet(
                            parentType = fragmentType,
                            selections = fragmentSelections,
                            fragmentMap = fragmentMap,
                            responsePath = responsePath,
                            enclosingRuntimePaths = composedPaths,
                        ).fields.map { field ->
                            field.copy(
                                condition = mergeConditions(field.condition, dirCond),
                            )
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
                    val inlineSelections = selection.selectionSet.selections

                    val dirCond = directivesToCondition(selection.directives)
                    if (dirCond == null) continue

                    val fragPaths =
                        if (selection.typeCondition != null) {
                            fragmentRuntimePaths(
                                responsePath = responsePath,
                                typeName = inlineTypeName,
                                schemaIndex = schemaIndex,
                            )
                        } else {
                            emptySet()
                        }

                    val composedPaths = composeRuntimePaths(
                        enclosingRuntimePaths,
                        fragPaths,
                    )

                    val inlineFields =
                        parseSelectionSet(
                            parentType = inlineType,
                            selections = inlineSelections,
                            fragmentMap = fragmentMap,
                            responsePath = responsePath,
                            enclosingRuntimePaths = composedPaths,
                        ).fields.map { field ->
                            val childRuntimePaths = composeRuntimePaths(
                                composedPaths,
                                field.runtimePaths,
                            )
                            field.copy(
                                runtimePaths = childRuntimePaths,
                                condition = mergeConditions(field.condition, dirCond),
                            )
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

        val normalizedFields = normalizeFields(rawFields)
            .sortedBy { it.responseName }
        return ResponseSelectionSet(
            parentType = parentType.name,
            responsePath = responsePath,
            fields = normalizedFields,
        )
    }

    private fun parseField(
        field: Field,
        parentType: SchemaType,
        fragmentMap: Map<String, FragmentDefinition>,
        parentResponsePath: ResponsePath,
        enclosingRuntimePaths: Set<RuntimePath>,
    ): ResponseFieldVariant? {
        val schemaName = field.name
        val responseName = field.alias ?: schemaName

        if (schemaName == "__typename") {
            return ResponseFieldVariant(
                responseName = responseName,
                schemaName = schemaName,
                outputType = GraphQLType.Named("String", nullable = false),
                runtimePaths = emptySet(),
            )
        }

        val fieldDef = requireNotNull(
            getOutputFields(parentType).find { it.name == schemaName },
        ) {
            "Field '$schemaName' not found on type '${parentType.name}'."
        }

        val condition = directivesToCondition(field.directives)
        if (condition == null) return null

        val typeName = resolveNamedType(fieldDef.type)
        val def = schemaIndex.definition(typeName)
        val childResponsePath = parentResponsePath + responseName
        val argsIdentity = normalizeArguments(field.arguments)

        val childSelections = field.selectionSet?.selections.orEmpty()
        val nestedSet: ResponseSelectionSet? =
            if (childSelections.isNotEmpty()) {
                val childType = when (def) {
                    is SchemaType.ObjectType,
                    is SchemaType.InterfaceType,
                    is SchemaType.UnionType,
                    -> resolveFragmentType(TypeName.newTypeName(typeName).build())
                    else -> parentType
                }

                parseSelectionSet(
                    parentType = childType,
                    selections = childSelections,
                    fragmentMap = fragmentMap,
                    responsePath = childResponsePath,
                    enclosingRuntimePaths = enclosingRuntimePaths,
                )
            } else {
                null
            }

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
                nestedSet.copy(fields = listOf(TYPE_NAME_FIELD) + nestedSet.fields)
            } else {
                nestedSet
            }

        return ResponseFieldVariant(
            responseName = responseName,
            schemaName = schemaName,
            outputType = fieldDef.type,
            argumentsIdentity = argsIdentity,
            condition = condition,
            selectionSet = finalSet,
            runtimePaths = emptySet(),
        )
    }

    /**
     * Normalizes raw fields: merges compatible variants, keeps
     * incompatible ones separate. Variants with empty runtimePaths
     * are never merged (they represent "applies to all").
     */
    private fun normalizeFields(
        fields: List<ResponseFieldVariant>,
    ): List<ResponseFieldVariant> {
        val grouped = linkedMapOf<String, MutableList<ResponseFieldVariant>>()
        for (field in fields) {
            grouped.getOrPut(field.responseName) { mutableListOf() }.add(field)
        }

        return grouped.flatMap { (_, group) ->
            if (group.size == 1) return@flatMap listOf(group.first())

            val remaining = group.toMutableList()
            val result = mutableListOf<ResponseFieldVariant>()

            while (remaining.isNotEmpty()) {
                val current = remaining.removeAt(0)
                var merged = current
                val iter = remaining.listIterator()
                while (iter.hasNext()) {
                    val other = iter.next()
                    if (canMergeCompactly(merged, other)) {
                        merged = mergeVariants(merged, other)
                        iter.remove()
                    }
                }
                result.add(merged)
            }
            result
        }
    }

    /**
     * Tests whether two variants can be merged. Two variants with
     * empty runtimePaths are never merged (preserves "applies to all").
     */
    private fun canMergeCompactly(
        left: ResponseFieldVariant,
        right: ResponseFieldVariant,
    ): Boolean {
        if (left.schemaName != right.schemaName) return false
        if (left.argumentsIdentity != right.argumentsIdentity) return false
        if (left.outputType != right.outputType) return false
        if (left.condition.mayBeAbsent != right.condition.mayBeAbsent) return false
        if (!runtimePathsOverlap(left.runtimePaths, right.runtimePaths)) return false
        // Prevent merging when one side is empty (applies-to-all) and
        // the other is scoped. When both are empty or both scoped, merge
        // is allowed if all other conditions pass.
        val oneEmpty = left.runtimePaths.isEmpty() != right.runtimePaths.isEmpty()
        if (oneEmpty) return false
        return true
    }

    private fun runtimePathsOverlap(
        left: Set<RuntimePath>,
        right: Set<RuntimePath>,
    ): Boolean {
        if (left.isEmpty() || right.isEmpty()) return true
        return left.any { l -> right.any { r -> !hasConflict(l, r) } }
    }

    private fun hasConflict(left: RuntimePath, right: RuntimePath): Boolean {
        return left.assignments.any { (path, type) ->
            val rightType = right.assignments[path]
            rightType != null && rightType != type
        }
    }

    private fun mergeVariants(
        left: ResponseFieldVariant,
        right: ResponseFieldVariant,
    ): ResponseFieldVariant {
        val mergedPaths = left.runtimePaths + right.runtimePaths
        val mergedSet: ResponseSelectionSet? =
            if (left.selectionSet != null && right.selectionSet != null) {
                ResponseSelectionSet(
                    parentType = left.selectionSet.parentType,
                    responsePath = left.selectionSet.responsePath,
                    fields = normalizeFields(
                        left.selectionSet.fields + right.selectionSet.fields,
                    ),
                )
            } else {
                left.selectionSet ?: right.selectionSet
            }
        return left.copy(runtimePaths = mergedPaths, selectionSet = mergedSet)
    }

    private fun resolveFragmentType(typeName: TypeName?): SchemaType {
        val name = requireNotNull(typeName?.name) { "Type condition must have a name." }
        val def = requireNotNull(schemaIndex.definition(name)) {
            "Type '$name' not found in schema."
        }
        return when (def) {
            is SchemaType.ObjectType -> def
            is SchemaType.InterfaceType -> def
            is SchemaType.UnionType -> def
            else -> throw IllegalArgumentException(
                "Fragment type condition '$name' is not a composite type.",
            )
        }
    }

    private fun getOutputFields(type: SchemaType): List<OutputField> = when (type) {
        is SchemaType.ObjectType -> type.fields
        is SchemaType.InterfaceType -> type.fields
        else -> emptyList()
    }

    private fun resolveNamedType(type: GraphQLType): String = when (type) {
        is GraphQLType.Named -> type.name
        is GraphQLType.List -> resolveNamedType(type.of)
    }

    private fun normalizeArguments(
        arguments: List<graphql.language.Argument>,
    ): String {
        if (arguments.isEmpty()) return ""
        return arguments.sortedBy { it.name }
            .joinToString("&") { "${it.name}=${renderValue(it.value)}" }
    }

    private fun renderValue(value: graphql.language.Value<*>): String = when (value) {
        is graphql.language.StringValue -> "\"${value.value}\""
        is graphql.language.IntValue -> value.value.toString()
        is graphql.language.FloatValue -> value.value.toString()
        is graphql.language.BooleanValue -> value.isValue.toString()
        is graphql.language.EnumValue -> value.name
        is graphql.language.VariableReference -> "\$${value.name}"
        is graphql.language.NullValue -> "null"
        is graphql.language.ArrayValue ->
            value.values.joinToString(",") { renderValue(it) }
        is graphql.language.ObjectValue ->
            value.objectFields.joinToString(",") {
                "${it.name}:${renderValue(it.value)}"
            }
        else -> value.toString()
    }

    /**
     * Converts directives to a [SelectionCondition].
     * Iterates ALL directives. Returns null if any excludes statically.
     */
    private fun directivesToCondition(
        directives: List<graphql.language.Directive>,
    ): SelectionCondition? {
        if (directives.isEmpty()) return SelectionCondition.UNCONDITIONAL
        var mayBeAbsent = false
        for (directive in directives) {
            val arg = directive.getArgument("if") ?: continue
            val value = arg.value
            when (directive.name) {
                "include" -> {
                    when {
                        value is graphql.language.VariableReference -> mayBeAbsent = true
                        value is graphql.language.BooleanValue && !value.isValue -> return null
                    }
                }
                "skip" -> {
                    when {
                        value is graphql.language.VariableReference -> mayBeAbsent = true
                        value is graphql.language.BooleanValue && value.isValue -> return null
                    }
                }
            }
        }
        return SelectionCondition(mayBeAbsent = mayBeAbsent)
    }

    private fun mergeConditions(
        base: SelectionCondition,
        override: SelectionCondition,
    ): SelectionCondition {
        return SelectionCondition(mayBeAbsent = base.mayBeAbsent || override.mayBeAbsent)
    }
}
