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

package co.anitrend.retrofit.graphql.codegen.schema

import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import graphql.language.AstPrinter
import graphql.language.Document
import graphql.language.Field
import graphql.language.InlineFragment
import graphql.language.OperationDefinition
import graphql.language.Selection
import graphql.language.SelectionSet
import graphql.parser.Parser

/**
 * Injects `__typename` fields into executable GraphQL operation documents
 * for abstract type selections (interfaces and unions).
 *
 * When the server returns data for an abstract field (interface or union),
 * it must include `__typename` so the client can discriminate the concrete
 * type. This injector uses [SchemaIndex] to identify abstract types and
 * ensures the document requests `__typename` for them.
 *
 * The injection is performed on the AST level: the document is parsed,
 * the AST is traversed recursively, and `__typename` is added to selection
 * sets whose parent field's output type is abstract. The resulting document
 * is serialized back to a GraphQL string.
 *
 * @property schemaIndex Schema metadata used to determine if a type is
 *   abstract (interface or union).
 */
class TypenameInjector(private val schemaIndex: SchemaIndex) {

    /**
     * Injects `__typename` fields into [operationDocument] for all abstract type
     * selections, then returns the transformed document as a GraphQL string.
     *
     * @param operationDocument The GraphQL operation document text (with fragments
     *   already resolved/inlined).
     * @param operationType Whether this is a query, mutation, or subscription.
     * @return The transformed document text with `__typename` injected where needed.
     * @throws IllegalArgumentException if no operation definition is found.
     */
    fun injectTypename(
        operationDocument: String,
        operationType: OperationDefinition.Operation,
    ): String {
        val document = Parser().parseDocument(operationDocument)
        val operationDef = document.definitions
            .filterIsInstance<OperationDefinition>()
            .firstOrNull()
            ?: throw IllegalArgumentException("No operation definition found in document")

        val rootTypeName = when (operationType) {
            OperationDefinition.Operation.QUERY -> schemaIndex.queryTypeName ?: "Query"
            OperationDefinition.Operation.MUTATION -> schemaIndex.mutationTypeName ?: "Mutation"
            OperationDefinition.Operation.SUBSCRIPTION -> schemaIndex.subscriptionTypeName ?: "Subscription"
        }

        val transformedSelections = injectInSelectionSet(
            operationDef.selectionSet,
            rootTypeName,
        )

        val transformedDef = operationDef.transform { builder ->
            builder.selectionSet(transformedSelections)
        }

        val transformedDoc = Document.newDocument()
            .definitions(listOf(transformedDef))
            .build()

        return AstPrinter.printAst(transformedDoc)
    }

    /**
     * Recursively injects `__typename` into a selection set.
     *
     * For each [Field] selection: resolves the field's output type from the schema.
     * If the field has a nested selection set and the output type is abstract
     * (interface or union), `__typename` is prepended to the nested selections.
     * The recursion continues into the child selection set.
     *
     * For [InlineFragment] selections: recurses into the fragment's selection set
     * using the fragment's type condition as the parent type.
     *
     * For [FragmentSpread] selections: passed through unchanged (they should not
     * be present after fragment resolution, but are handled defensively).
     *
     * @param selectionSet The selection set to process, or null.
     * @param parentTypeName The schema type name of the enclosing field/type.
     * @return A new selection set with injected `__typename` fields, or null.
     */
    private fun injectInSelectionSet(
        selectionSet: SelectionSet?,
        parentTypeName: String,
    ): SelectionSet? {
        if (selectionSet == null) return null
        val newSelections = mutableListOf<Selection<*>>()

        for (selection in selectionSet.selections) {
            when (selection) {
                is Field -> {
                    val field = selection
                    val objectType = schemaIndex.objectType(parentTypeName)
                    val outputField = objectType?.fields?.find { it.name == field.name }
                    // Resolve base type name, unwrapping List wrappers
                    val resolvedType = outputField?.type?.let { resolveBaseTypeName(it) }

                    // Inject __typename into child selections if parent is abstract
                    var childSet = field.selectionSet
                    if (childSet != null && resolvedType != null) {
                        val injected = injectInSelectionSet(childSet, resolvedType)
                        if (injected != null) {
                            childSet = injected
                            val isAbstract = schemaIndex.isAbstractType(resolvedType)
                            if (isAbstract) {
                                val selections = childSet.selections.toMutableList()
                                val hasTypename = selections.any { s ->
                                    s is Field && s.name == "__typename"
                                }
                                if (!hasTypename) {
                                    selections.add(0, Field.newField("__typename").build())
                                }
                                childSet = SelectionSet.newSelectionSet()
                                    .selections(selections)
                                    .build()
                            }
                        }
                    }

                    newSelections.add(
                        field.transform { builder ->
                            builder.selectionSet(childSet)
                        },
                    )
                }

                is InlineFragment -> {
                    val fragment = selection
                    val typeName = fragment.typeCondition?.name ?: parentTypeName
                    val transformedChildSet = injectInSelectionSet(
                        fragment.selectionSet,
                        typeName,
                    )
                    newSelections.add(
                        fragment.transform { builder ->
                            builder.selectionSet(transformedChildSet)
                        },
                    )
                }

                is graphql.language.FragmentSpread -> {
                    // After fragment resolution, spread nodes should be gone,
                    // but handle them gracefully
                    newSelections.add(selection)
                }
            }
        }

        return SelectionSet.newSelectionSet()
            .selections(newSelections)
            .build()
    }

    /**
     * Resolves the base type name from a [GraphQLType], unwrapping list wrappers.
     *
     * @param type The type to resolve.
     * @return The named type at the core of the type reference, or null if
     *   the type cannot be resolved to a named type.
     */
    private fun resolveBaseTypeName(type: GraphQLType): String? {
        return when (type) {
            is GraphQLType.Named -> type.name
            is GraphQLType.List -> resolveBaseTypeName(type.of)
        }
    }
}
