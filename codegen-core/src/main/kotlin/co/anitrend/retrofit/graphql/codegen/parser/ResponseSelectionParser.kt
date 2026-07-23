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
import co.anitrend.retrofit.graphql.codegen.model.ResponseField
import co.anitrend.retrofit.graphql.codegen.model.ResponseSelectionSet
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
     */
    private fun parseSelectionSet(
        parentType: SchemaType.ObjectType,
        selections: List<Selection<*>>,
        fragmentMap: Map<String, FragmentDefinition>,
    ): ResponseSelectionSet {
        val rawFields = mutableListOf<ResponseField>()

        for (selection in selections) {
            when (selection) {
                is Field -> {
                    rawFields.add(
                        parseField(
                            field = selection,
                            parentType = parentType,
                            fragmentMap = fragmentMap,
                        ),
                    )
                }
                is FragmentSpread -> {
                    val fragment =
                        requireNotNull(fragmentMap[selection.name]) {
                            "Fragment '${selection.name}' not found."
                        }
                    val fragmentType =
                        resolveObjectType(fragment.typeCondition)
                    val fragmentSelections =
                        fragment.selectionSet?.selections.orEmpty()
                    val spreadFields =
                        parseSelectionSet(
                            parentType = fragmentType,
                            selections = fragmentSelections,
                            fragmentMap = fragmentMap,
                        ).fields.map { field ->
                            field.copy(
                                condition = mergeConditions(
                                    field.condition,
                                    directivesToCondition(
                                        selection.directives,
                                    ),
                                ),
                            )
                        }
                    rawFields.addAll(spreadFields)
                }
                is InlineFragment -> {
                    val inlineType =
                        resolveObjectType(selection.typeCondition)
                    val inlineSelections =
                        selection.selectionSet?.selections.orEmpty()
                    val inlineFields =
                        parseSelectionSet(
                            parentType = inlineType,
                            selections = inlineSelections,
                            fragmentMap = fragmentMap,
                        ).fields.map { field ->
                            field.copy(
                                condition = mergeConditions(
                                    field.condition,
                                    directivesToCondition(
                                        selection.directives,
                                    ),
                                ),
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

        // Merge repeated fields by responseName, then sort deterministically
        val mergedFields = mergeFields(rawFields).sortedBy { it.responseName }
        return ResponseSelectionSet(
            parentType = parentType.name,
            fields = mergedFields,
        )
    }

    /**
     * Parses a single field selection against a parent object type.
     */
    private fun parseField(
        field: Field,
        parentType: SchemaType.ObjectType,
        fragmentMap: Map<String, FragmentDefinition>,
    ): ResponseField {
        val schemaName = field.name
        val responseName = field.alias ?: schemaName

        val fieldDef =
            requireNotNull(
                parentType.fields.find { it.name == schemaName },
            ) {
                "Field '$schemaName' not found on type '${parentType.name}'."
            }

        val condition = directivesToCondition(field.directives)

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

        // Parse nested selections if present
        val childSelections = field.selectionSet?.selections.orEmpty()
        val nestedSet: ResponseSelectionSet? =
            if (childSelections.isNotEmpty()) {
                val childTypeName =
                    when (def) {
                        is SchemaType.InterfaceType,
                        is SchemaType.UnionType,
                        -> possibleTypes.firstOrNull() ?: typeName
                        else -> typeName
                    }
                val childType =
                    resolveObjectType(
                        TypeName.newTypeName(childTypeName).build(),
                    )
                parseSelectionSet(
                    parentType = childType,
                    selections = childSelections,
                    fragmentMap = fragmentMap,
                )
            } else {
                null
            }

        return ResponseField(
            responseName = responseName,
            schemaName = schemaName,
            outputType = fieldDef.type,
            condition = condition,
            possibleTypes = possibleTypes,
            selectionSet = nestedSet,
        )
    }

    /**
     * Merges fields that share the same [ResponseField.responseName].
     * Compatible fields (same schemaName, same top-level output type)
     * have their nested selection sets combined. Incompatible fields
     * cause an error.
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

            // Multiple fields with same responseName — must be compatible
            val first = group.first()
            for (other in group.drop(1)) {
                require(other.schemaName == first.schemaName) {
                    "Incompatible field merge at '$responseName': " +
                        "'${first.schemaName}' and '${other.schemaName}' " +
                        "have the same response name but different schema names."
                }
            }

            // Merge nested selection sets
            val mergedSelectionSet =
                group
                    .mapNotNull { it.selectionSet }
                    .fold(null as ResponseSelectionSet?) { acc, set ->
                        if (acc == null) {
                            set
                        } else {
                            ResponseSelectionSet(
                                parentType = acc.parentType,
                                fields = mergeFields(
                                    acc.fields + set.fields,
                                ),
                            )
                        }
                    }

            first.copy(selectionSet = mergedSelectionSet)
        }
    }

    /**
     * Resolves a [TypeName] to an [SchemaType.ObjectType] in the
     * schema index, throwing if the type is not found or is not an
     * object type.
     */
    private fun resolveObjectType(
        typeName: TypeName?,
    ): SchemaType.ObjectType {
        val name =
            requireNotNull(typeName?.name) {
                "Type condition must have a name."
            }
        return requireNotNull(schemaIndex.objectType(name)) {
            "Object type '$name' not found in schema."
        }
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
     */
    private fun directivesToCondition(
        directives: List<graphql.language.Directive>,
    ): SelectionCondition {
        if (directives.isEmpty()) return SelectionCondition.UNCONDITIONAL

        var isConditional = false
        var skipIf = false
        var variableName: String? = null

        for (directive in directives) {
            when (directive.name) {
                "include" -> {
                    isConditional = true
                    skipIf = false
                    variableName =
                        extractDirectiveVariable(directive, "if")
                }
                "skip" -> {
                    isConditional = true
                    skipIf = true
                    variableName =
                        extractDirectiveVariable(directive, "if")
                }
            }
        }

        return SelectionCondition(
            isConditional = isConditional,
            skipIf = skipIf,
            variableName = variableName,
        )
    }

    private fun extractDirectiveVariable(
        directive: graphql.language.Directive,
        argumentName: String,
    ): String? {
        val arg =
            directive.getArgument(argumentName)
                ?: return null
        return when (val value = arg.value) {
            is graphql.language.VariableReference -> value.name
            is graphql.language.BooleanValue -> null // literal true/false, not conditional
            else -> null
        }
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
