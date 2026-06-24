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
import graphql.language.AstPrinter
import graphql.language.AstTransformer
import graphql.language.FragmentDefinition
import graphql.language.FragmentSpread
import graphql.language.InlineFragment
import graphql.language.NodeTraverser
import graphql.language.NodeVisitorStub
import graphql.parser.Parser
import graphql.util.TraversalControl
import graphql.util.TraverserContext
import graphql.util.TreeTransformerUtil

/**
 * Resolves fragment references in operations by inlining fragment definitions.
 *
 * Fragment references (`...FragmentName`) are replaced with [InlineFragment] nodes
 * containing the fragment's selection set. The [AstTransformer] recursively visits
 * transformed nodes, so nested fragment references are resolved automatically.
 *
 * The resolution proceeds in distinct phases:
 * 1. **Transitive reachability** — computes the full transitive closure of fragment
 *    names reachable from each operation's direct fragment spreads. Missing fragment
 *    definitions cause immediate failure.
 * 2. **Cycle detection** — validates that no cycles exist among reachable fragments.
 * 3. **Inlining** — replaces each [FragmentSpread] with an [InlineFragment] carrying
 *    the definition's selection set and type condition.
 * 4. **Invariant check** — verifies no named fragment spreads remain after inlining.
 */
class FragmentResolver {
    private val parser = Parser()

    /**
     * Resolves all fragment references in all operations.
     *
     * @param operations The operations to process.
     * @param fragments All fragment definitions available for inlining, keyed by name.
     * @return Operations with fragments inlined into their document text.
     */
    fun resolve(
        operations: List<GraphQLOperationInfo>,
        fragments: Map<String, GraphQLFragmentInfo>,
    ): List<GraphQLOperationInfo> {
        if (fragments.isEmpty()) return operations

        // Pre-parse all fragment definitions into FragmentDefinition AST nodes
        val fragmentDefinitions =
            fragments.mapValues { (_, info) ->
                parser.parseDocument(info.document)
                    .definitions
                    .filterIsInstance<FragmentDefinition>()
                    .first()
            }

        return operations.map { operation ->
            val flattenedDocument = flattenFragments(operation.document, fragmentDefinitions)
            operation.copy(document = flattenedDocument)
        }
    }

    /**
     * Flattens all fragment references in a document string by inlining their definitions
     * as [InlineFragment] nodes.
     *
     * Resolution phases:
     * 1. Parse document to AST.
     * 2. Compute all transitively-reachable fragment names from the operation's
     *    direct fragment spreads, validating that every referenced fragment exists.
     * 3. Detect cycles among the reachable fragments.
     * 4. Transform: replace each [FragmentSpread] with an inline fragment.
     * 5. Validate: no named fragment spreads remain (defensive invariant check).
     */
    private fun flattenFragments(
        documentText: String,
        fragmentDefinitions: Map<String, FragmentDefinition>,
    ): String {
        val document = parser.parseDocument(documentText)

        // Phase 1: Compute full transitive closure of reachable fragment names
        val reachableNames = computeReachableFragments(document, fragmentDefinitions)
        if (reachableNames.isEmpty()) return documentText

        // Phase 2: Validate no cycles among reachable fragments
        detectCycles(reachableNames, fragmentDefinitions)

        // Phase 3: Inline fragments using AstTransformer
        val relevantDefinitions = fragmentDefinitions.filterKeys { it in reachableNames }

        val transformer = AstTransformer()
        val transformed =
            transformer.transform(
                document,
                object : NodeVisitorStub() {
                    @Suppress("UNCHECKED_CAST")
                    override fun visitFragmentSpread(
                        node: FragmentSpread,
                        context: TraverserContext<graphql.language.Node<*>>,
                    ): TraversalControl {
                        val definition =
                            relevantDefinitions[node.name]
                                ?: return TraversalControl.CONTINUE

                        // Replace spread with an inline fragment that carries the definition's
                        // selection set and type condition. The AstTransformer will continue
                        // traversing into the children of this new node, resolving any nested
                        // FragmentSpread references automatically.
                        val inlineFragment =
                            InlineFragment.newInlineFragment()
                                .typeCondition(definition.typeCondition)
                                .selectionSet(definition.selectionSet)
                                .build()

                        return TreeTransformerUtil.changeNode(
                            context as TraverserContext<InlineFragment>,
                            inlineFragment,
                        )
                    }
                },
            )

        // Phase 4: Defensive invariant check — no named fragment spreads should
        // remain after inlining. This catches bugs where a fragment spread name
        // was not found in relevantDefinitions (e.g. missing from transitive closure).
        val remainingSpreads = collectSpreadNames(transformed)
        if (remainingSpreads.isNotEmpty()) {
            throw IllegalStateException(
                "Incomplete fragment resolution: the following fragment spread(s) remain " +
                    "unresolved after inlining: ${remainingSpreads.joinToString(", ")}. " +
                    "Ensure all referenced fragment definitions are included in the " +
                    "fragment definitions map.",
            )
        }

        // Serialize the transformed AST back to a GraphQL document string
        return AstPrinter.printAst(transformed)
    }

    /**
     * Computes the full transitive closure of fragment names reachable from
     * the operation document's fragment spreads, following fragment spreads into
     * fragment definitions.
     *
     * Uses a BFS-style queue to traverse the fragment graph. Each fragment definition
     * is scanned for nested fragment spreads, and those names are added to the queue
     * for further traversal. This process continues until all reachable fragments
     * have been visited.
     *
     * @throws IllegalArgumentException if a referenced fragment is not defined in
     *  [fragmentDefinitions].
     */
    private fun computeReachableFragments(
        document: graphql.language.Document,
        fragmentDefinitions: Map<String, FragmentDefinition>,
    ): Set<String> {
        val directNames = collectSpreadNames(document)
        if (directNames.isEmpty()) return emptySet()

        val reachable = mutableSetOf<String>()
        val queue = ArrayDeque(directNames)

        while (queue.isNotEmpty()) {
            val name = queue.removeFirst()
            if (!reachable.add(name)) continue

            val definition =
                fragmentDefinitions[name]
                    ?: throw IllegalArgumentException(
                        "Fragment '$name' is referenced but not defined. " +
                            "Available fragments: ${fragmentDefinitions.keys}",
                    )

            // Collect nested fragment spreads within this fragment's selection set
            val nestedNames = collectSpreadNames(definition)
            queue.addAll(nestedNames)
        }

        return reachable
    }

    /**
     * Detects cycles among the given fragment names by traversing the fragment
     * dependency graph with DFS. A cycle is detected when a fragment is encountered
     * again while it is still on the current DFS recursion stack.
     *
     * @throws IllegalArgumentException with the cycle path if a cycle is found.
     */
    private fun detectCycles(
        fragmentNames: Set<String>,
        fragmentDefinitions: Map<String, FragmentDefinition>,
    ) {
        val visited = mutableSetOf<String>()
        val inStack = mutableSetOf<String>()

        fun dfs(name: String) {
            if (name in inStack) {
                throw IllegalArgumentException(
                    "Cyclic fragment reference detected: $name",
                )
            }
            if (name in visited) return

            val definition = fragmentDefinitions[name]
                ?: throw IllegalArgumentException(
                    "Fragment '$name' is referenced but not defined. " +
                        "Available fragments: ${fragmentDefinitions.keys}",
                )

            inStack.add(name)
            visited.add(name)

            val nestedNames = collectSpreadNames(definition)
            // Only follow references that exist in our reachable fragment set
            for (nestedName in nestedNames) {
                if (nestedName in fragmentNames) {
                    dfs(nestedName)
                }
            }

            inStack.remove(name)
        }

        fragmentNames.forEach { dfs(it) }
    }

    /**
     * Collects all [FragmentSpread] names from any AST node (document, definition,
     * selection set) using a depth-first traversal.
     */
    private fun collectSpreadNames(node: graphql.language.Node<*>): Set<String> {
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
            node,
        )
        return names
    }
}
