package co.anitrend.retrofit.graphql.codegen.parser

import co.anitrend.retrofit.graphql.codegen.model.GraphQLFragmentInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import co.anitrend.retrofit.graphql.codegen.model.OperationType
import graphql.language.FragmentDefinition
import graphql.language.OperationDefinition
import graphql.parser.Parser
import java.io.File

/**
 * Parses .graphql files into structured [GraphQLOperationInfo] and [GraphQLFragmentInfo] models.
 */
class GraphQLDocumentParser {

    private val parser = Parser()

    /**
     * Parses all operations from a single .graphql file.
     * A file may contain multiple operations.
     *
     * @param file The .graphql file to parse.
     * @return All operations found in the file.
     */
    fun parseOperations(file: File): List<GraphQLOperationInfo> {
        val source = file.readText()
        val document = parser.parseDocument(source)

        return document.definitions
            .filterIsInstance<OperationDefinition>()
            .map { definition ->
                val name = definition.name
                    ?: throw IllegalArgumentException(
                        "Anonymous operations are not allowed. " +
                            "Provide a name for the operation in ${file.name}."
                    )
                val type = OperationType.fromGraphQLJava(definition.operation.name)
                val originalDocument = extractDocumentText(source, definition)

                GraphQLOperationInfo(
                    name = name,
                    type = type,
                    document = originalDocument,
                    sourceFile = file.name,
                )
            }
    }

    /**
     * Parses all fragment definitions from a single .graphql file.
     *
     * @param file The .graphql file to parse.
     * @return All fragment definitions found in the file.
     */
    fun parseFragments(file: File): List<GraphQLFragmentInfo> {
        val source = file.readText()
        val document = parser.parseDocument(source)

        return document.definitions
            .filterIsInstance<FragmentDefinition>()
            .map { definition ->
                val fragmentText = extractDocumentText(source, definition)

                GraphQLFragmentInfo(
                    name = definition.name,
                    document = fragmentText,
                )
            }
    }

    /**
     * Extracts the source text span for a definition from the original source.
     * Uses graphql-java's [graphql.language.AstPrinter] for deterministic output.
     */
    private fun extractDocumentText(
        source: String,
        definition: graphql.language.Definition<*>,
    ): String {
        return graphql.language.AstPrinter.printAst(definition)
    }
}
