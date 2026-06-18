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

import co.anitrend.retrofit.graphql.codegen.model.GraphQLFragmentInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLVariableInfo
import co.anitrend.retrofit.graphql.codegen.model.OperationType
import graphql.language.FragmentDefinition
import graphql.language.OperationDefinition
import graphql.language.VariableDefinition
import graphql.parser.Parser
import java.io.File

/**
 * Parses .graphql files into structured [GraphQLOperationInfo] and [GraphQLFragmentInfo] models.
 */
class GraphQLDocumentParser {
    private val parser = Parser()
    private val schemaTypeParser = SchemaParser()

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
                val name =
                    definition.name
                        ?: throw IllegalArgumentException(
                            "Anonymous operations are not allowed. " +
                                "Provide a name for the operation in ${file.name}.",
                        )
                val type = OperationType.fromGraphQLJava(definition.operation.name)
                val originalDocument = extractDocumentText(source, definition)
                val variables = definition.variableDefinitions.map { parseVariable(it) }

                GraphQLOperationInfo(
                    name = name,
                    type = type,
                    document = originalDocument,
                    sourceFile = file.name,
                    variables = variables,
                )
            }
    }

    /**
     * Converts a [VariableDefinition] AST node to [GraphQLVariableInfo].
     */
    private fun parseVariable(definition: VariableDefinition): GraphQLVariableInfo {
        return GraphQLVariableInfo(
            name =
            definition.name
                ?: throw IllegalArgumentException("Variable definition must have a name"),
            type = schemaTypeParser.toGraphQLType(definition.type),
            defaultValue =
            definition.defaultValue?.let {
                graphql.language.AstPrinter.printAst(it)
            },
        )
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
