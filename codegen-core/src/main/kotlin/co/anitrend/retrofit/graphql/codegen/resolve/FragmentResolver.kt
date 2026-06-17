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
 * containing the fragment's selection set. The AstTransformer recursively handles
 * nested fragments.
 *
 * Cyclic fragment references are detected and rejected before transformation.
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
        val fragmentDefinitions = fragments.mapValues { (_, info) ->
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
     * as [InlineFragment] nodes. Uses [AstTransformer] which recursively visits transformed
     * nodes, so nested fragment references are resolved automatically.
     */
    private fun flattenFragments(
        documentText: String,
        fragmentDefinitions: Map<String, FragmentDefinition>,
    ): String {
        val document = parser.parseDocument(documentText)

        // Validate: no missing or cyclic fragments
        val referencedNames = collectReferencedNames(document)
        checkForCycles(referencedNames, fragmentDefinitions)

        if (referencedNames.isEmpty()) return documentText

        // Filter to only fragments actually referenced from this document
        val relevantDefinitions = fragmentDefinitions.filterKeys { it in referencedNames }

        val transformer = AstTransformer()
        val transformed = transformer.transform(document, object : NodeVisitorStub() {
            @Suppress("UNCHECKED_CAST")
            override fun visitFragmentSpread(
                node: FragmentSpread,
                context: TraverserContext<graphql.language.Node<*>>,
            ): TraversalControl {
                val definition = relevantDefinitions[node.name]
                    ?: return TraversalControl.CONTINUE

                // Replace spread with an inline fragment that carries the definition's
                // selection set and type condition. The AstTransformer will continue
                // traversing into the children of this new node, resolving any nested
                // FragmentSpread references automatically.
                val inlineFragment = InlineFragment.newInlineFragment()
                    .typeCondition(definition.typeCondition)
                    .selectionSet(definition.selectionSet)
                    .build()

                return TreeTransformerUtil.changeNode(
                    context as TraverserContext<InlineFragment>,
                    inlineFragment,
                )
            }
        })

        // Re-print with deterministic formatting
        return AstPrinter.printAst(transformed)
    }

    private fun collectReferencedNames(
        document: graphql.language.Document,
    ): Set<String> {
        val names = mutableSetOf<String>()
        NodeTraverser().depthFirst(object : NodeVisitorStub() {
            override fun visitFragmentSpread(
                node: FragmentSpread,
                context: TraverserContext<graphql.language.Node<*>>,
            ): TraversalControl {
                names.add(node.name)
                return TraversalControl.CONTINUE
            }
        }, document)
        return names
    }

    /**
     * Detects cycles and missing definitions in fragment references.
     * Throws [IllegalArgumentException] with a descriptive message on failure.
     */
    private fun checkForCycles(
        fragmentNames: Set<String>,
        fragmentDefinitions: Map<String, FragmentDefinition>,
    ) {
        for (name in fragmentNames) {
            val definition = fragmentDefinitions[name]
                ?: throw IllegalArgumentException(
                    "Fragment '$name' is referenced but not defined. " +
                        "Available fragments: ${fragmentDefinitions.keys}"
                )
        }

        val visited = mutableSetOf<String>()
        val inStack = mutableSetOf<String>()

        fun dfs(name: String) {
            if (name in inStack) {
                throw IllegalArgumentException(
                    "Cyclic fragment reference detected: $name"
                )
            }
            if (name in visited) return

            val definition = fragmentDefinitions[name] ?: return
            inStack.add(name)
            visited.add(name)

            // Find fragment references within this fragment definition
            NodeTraverser().depthFirst(object : NodeVisitorStub() {
                override fun visitFragmentSpread(
                    node: FragmentSpread,
                    context: TraverserContext<graphql.language.Node<*>>,
                ): TraversalControl {
                    dfs(node.name)
                    return TraversalControl.CONTINUE
                }
            }, definition)

            inStack.remove(name)
        }

        fragmentNames.forEach { dfs(it) }
    }
}
