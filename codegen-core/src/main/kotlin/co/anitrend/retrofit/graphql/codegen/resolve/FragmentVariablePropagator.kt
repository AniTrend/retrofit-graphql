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

package co.anitrend.retrofit.graphql.codegen.resolve

import co.anitrend.retrofit.graphql.codegen.model.GraphQLFragmentInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.GraphQLVariableInfo
import graphql.language.AstPrinter
import graphql.language.AstTransformer
import graphql.language.FragmentSpread
import graphql.language.NodeTraverser
import graphql.language.NodeVisitorStub
import graphql.language.OperationDefinition
import graphql.language.TypeName
import graphql.language.VariableDefinition
import graphql.parser.Parser
import graphql.util.TraversalControl
import graphql.util.TraverserContext
import graphql.util.TreeTransformerUtil

/**
 * Result of fragment variable propagation.
 *
 * @property operations Operations with auto-propagated variables added.
 * @property warnings Warning messages for each auto-propagated variable.
 */
data class PropagationResult(
    val operations: List<GraphQLOperationInfo>,
    val warnings: List<String>,
)

/**
 * Propagates fragment variable usages into consuming operations.
 *
 * GraphQL fragments can *use* variables (e.g. `age(format: $format)`) but cannot
 * *define* them — the consuming operation must declare them. This class detects
 * variables used by fragments that an operation spreads, and auto-propagates any
 * missing ones into the operation's [GraphQLOperationInfo.variables] list **and**
 * updates the operation document text to include the variable declarations.
 *
 * A warning is emitted for each auto-propagated variable so users know they should
 * declare it explicitly in their operation definition.
 *
 * Auto-propagated variables default to `String?` (nullable String). Type inference
 * from the schema is a planned follow-up.
 */
class FragmentVariablePropagator {
    private val parser = Parser()

    /**
     * Propagates fragment variable usages into consuming operations.
     *
     * @param operations The operations to process.
     * @param fragments All fragment definitions, keyed by name.
     * @return A [PropagationResult] with updated operations and warnings.
     */
    fun propagate(
        operations: List<GraphQLOperationInfo>,
        fragments: Map<String, GraphQLFragmentInfo>,
    ): PropagationResult {
        if (fragments.isEmpty()) {
            return PropagationResult(operations, emptyList())
        }

        val warnings = mutableListOf<String>()
        val resolvedOperations = operations.map { operation ->
            val spreadNames = collectSpreadNames(operation.document)
            if (spreadNames.isEmpty()) {
                operation
            } else {
                // Collect variable usages from all reachable fragments (including nested)
                // Returns a map of variableName -> set of fragment names that use it
                val fragmentVariableUsages = collectReachableVariableUsages(spreadNames, fragments)

                // Find which ones are missing from the operation's declared variables
                val existingNames = operation.variables.map { it.name }.toSet()
                val missingVariables = fragmentVariableUsages.keys.filter { it !in existingNames }

                // Check for type conflicts on variables that ARE declared but with non-String type
                val existingVarByName = operation.variables.associateBy { it.name }
                for ((varName, sourceFragments) in fragmentVariableUsages) {
                    if (varName in existingNames) {
                        val existingType = existingVarByName[varName]!!.type
                        if (existingType is GraphQLType.Named && existingType.name != "String") {
                            warnings.add(
                                "Operation '${operation.name}' declares '\$$varName' as ${existingType.name}" +
                                    (if (!existingType.nullable) "!" else "") +
                                    " but fragment(s) ${sourceFragments.joinToString(", ") { "'$it'" }} use it. " +
                                    "Consider verifying the type matches your fragment usage.",
                            )
                        }
                    }
                }

                if (missingVariables.isEmpty()) {
                    operation
                } else {
                    // Emit warnings for auto-propagated variables
                    for (varName in missingVariables) {
                        val sourceFragments = fragmentVariableUsages[varName].orEmpty()
                        warnings.add(
                            "Operation '${operation.name}' uses fragment(s) ${sourceFragments.joinToString(", ") { "'$it'" }} " +
                                "which reference variable '\$$varName', but the operation does not declare it. " +
                                "Auto-propagating as 'String?'. " +
                                "Consider declaring '\$$varName' explicitly in the operation definition.",
                        )
                    }

                    val propagatedVars = missingVariables.map { varName ->
                        GraphQLVariableInfo(
                            name = varName,
                            type = GraphQLType.Named("String", nullable = true),
                        )
                    }

                    // Update both the variables list AND the document text
                    val updatedDocument = addVariableDeclarations(operation.document, propagatedVars)

                    operation.copy(
                        document = updatedDocument,
                        variables = operation.variables + propagatedVars,
                    )
                }
            }
        }

        return PropagationResult(resolvedOperations, warnings)
    }

    /**
     * Collects all fragment spread names referenced in an operation document.
     */
    private fun collectSpreadNames(documentText: String): Set<String> {
        val document = parser.parseDocument(documentText)
        val names = mutableSetOf<String>()

        NodeTraverser().depthFirst(
            object : NodeVisitorStub() {
                override fun visitFragmentSpread(
                    node: FragmentSpread,
                    context: TraverserContext<graphql.language.Node<*>>,
                ): TraversalControl {
                    names.add(node.name)
                    return TraversalControl.CONTINUE
                }
            },
            document,
        )

        return names
    }

    /**
     * Collects variable usages from all fragments reachable from the given
     * spread names, including transitively referenced fragments (nested spreads).
     *
     * @return A map of variable name -> set of fragment names that use it.
     */
    private fun collectReachableVariableUsages(
        spreadNames: Set<String>,
        fragments: Map<String, GraphQLFragmentInfo>,
    ): Map<String, Set<String>> {
        val visited = mutableSetOf<String>()
        val variableToFragments = mutableMapOf<String, MutableSet<String>>()

        fun visit(name: String) {
            if (name in visited) return
            visited.add(name)

            val fragment = fragments[name] ?: return
            for (varName in fragment.variableUsages) {
                variableToFragments.getOrPut(varName) { mutableSetOf() }.add(name)
            }

            // Find nested fragment spreads within this fragment
            val nestedSpreads = collectSpreadNames(fragment.document)
            for (nested in nestedSpreads) {
                visit(nested)
            }
        }

        spreadNames.forEach { visit(it) }
        return variableToFragments
    }

    /**
     * Adds variable definitions to the operation document's AST and returns
     * the re-printed document text.
     *
     * This ensures the generated document string sent to the GraphQL server
     * includes declarations like `query GetUser($format: String)` for all
     * auto-propagated variables.
     */
    private fun addVariableDeclarations(
        documentText: String,
        newVars: List<GraphQLVariableInfo>,
    ): String {
        if (newVars.isEmpty()) return documentText

        val document = parser.parseDocument(documentText)

        val newVarDefs = newVars.map { v ->
            VariableDefinition.newVariableDefinition()
                .name(v.name)
                .type(TypeName.newTypeName().name("String").build())
                .build()
        }

        val transformer = AstTransformer()
        val transformed =
            transformer.transform(
                document,
                object : NodeVisitorStub() {
                    override fun visitOperationDefinition(
                        node: OperationDefinition,
                        context: TraverserContext<graphql.language.Node<*>>,
                    ): TraversalControl {
                        val updatedOp =
                            node.transform { builder ->
                                builder.variableDefinitions(
                                    node.variableDefinitions + newVarDefs,
                                )
                            }
                        return TreeTransformerUtil.changeNode(context, updatedOp)
                    }
                },
            )

        return AstPrinter.printAst(transformed)
    }
}