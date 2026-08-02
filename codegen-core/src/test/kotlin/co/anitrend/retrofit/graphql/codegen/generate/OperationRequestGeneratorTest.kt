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

package co.anitrend.retrofit.graphql.codegen.generate

import co.anitrend.retrofit.graphql.codegen.generate.helper.CompilationHelper
import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.GraphQLVariableInfo
import co.anitrend.retrofit.graphql.codegen.model.OperationType
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests that generated operation request helpers use the backend-neutral
 * `GraphQLOperationRequest` contract from `:api` and never the legacy
 * serializer-coupled `GraphQLRequest`.
 */
class OperationRequestGeneratorTest {

    private val packageName = "com.example"

    private fun operation(
        name: String,
        variables: List<GraphQLVariableInfo>,
    ) = GraphQLOperationInfo(
        name = name,
        type = OperationType.QUERY,
        document = "query $name(\$first: Int) { items(first: \$first) { id } }",
        sourceFile = "$name.graphql",
        variables = variables,
    )

    @Test
    fun `request helper returns GraphQLOperationRequest with variables`() {
        val op = operation(
            name = "GetItems",
            variables = listOf(
                GraphQLVariableInfo("first", GraphQLType.Named(name = "Int", nullable = false)),
            ),
        )

        val source = OperationRequestGenerator.generate(op, packageName, emptyMap(), SchemaIndex.EMPTY).toString()

        assertTrue(source.contains("import co.anitrend.retrofit.graphql.model.request.GraphQLOperationRequest"))
        assertTrue(
            source.contains(
                "public fun request(first: Int): GraphQLOperationRequest<GetItemsVariables>",
            ),
        )
        assertTrue(
            source.contains(
                "GraphQLOperationRequest(query = document, operationName = name, variables = GetItemsVariables(first = first))",
            ),
        )
        assertFalse(source.contains("GraphQLRequest"))
        assertFalse(source.contains("co.anitrend.retrofit.graphql.model.GraphQLRequest"))
    }

    @Test
    fun `request helper preserves operation document hash and registry semantics`() {
        val op = operation(
            name = "GetItems",
            variables = listOf(
                GraphQLVariableInfo("first", GraphQLType.Named(name = "Int", nullable = false)),
            ),
        )

        val source = OperationRequestGenerator.generate(op, packageName, emptyMap(), SchemaIndex.EMPTY).toString()

        assertTrue(source.contains("public object GetItems : GraphQLOperation<GetItemsVariables>"))
        assertTrue(source.contains("override val name: String = GraphQLOperations.Query.GetItems"))
        assertTrue(source.contains("override val document: String = GraphQLDocuments.GetItems"))
        assertTrue(source.contains("override val sha256Hash: String = GraphQLHashes.GetItems"))
    }

    @Test
    fun `no variable operation implements GraphQLNoVarOperation and has no request function`() {
        val op = operation(name = "GetCurrentUser", variables = emptyList())

        val source = OperationRequestGenerator.generate(op, packageName, emptyMap(), SchemaIndex.EMPTY).toString()

        assertTrue(source.contains("public object GetCurrentUser : GraphQLNoVarOperation"))
        assertFalse(source.contains("fun request"))
        assertFalse(source.contains("GraphQLOperationRequest"))
        assertTrue(source.contains("override val name: String = GraphQLOperations.Query.GetCurrentUser"))
    }

    @Test
    fun `request helper with variables compiles against neutral contract stubs`() {
        val op = operation(
            name = "GetItems",
            variables = listOf(
                GraphQLVariableInfo("first", GraphQLType.Named(name = "Int", nullable = false)),
            ),
        )

        // Compile the full generated artifact set: constants, documents, hashes,
        // variables, and the request helper, so helper references to
        // GraphQLOperations/GraphQLDocuments/GraphQLHashes resolve.
        val requestSpec = OperationRequestGenerator.generate(op, packageName, emptyMap(), SchemaIndex.EMPTY)
        val variablesSpec = VariableClassGenerator.generate(op, packageName, emptyMap(), SchemaIndex.EMPTY)!!
        val operationsSpec = OperationConstantsGenerator.generate(listOf(op), packageName)
        val documentsSpec = DocumentGenerator.generate(listOf(op), packageName)
        val hashesSpec = HashConstantsGenerator.generate(listOf(op), packageName)

        CompilationHelper.compile(
            fileSpecs = listOf(requestSpec, variablesSpec, operationsSpec, documentsSpec, hashesSpec),
            includeGraphQLVariablesStub = true,
        ).use { result ->
            // Request object implements GraphQLOperation and exposes the neutral request factory.
            val operationClass = result.classLoader.loadClass("$packageName.GetItems")
            val operationInterface =
                result.classLoader.loadClass("co.anitrend.retrofit.graphql.model.GraphQLOperation")
            assertTrue(
                "Generated operation must implement GraphQLOperation",
                operationInterface.isAssignableFrom(operationClass),
            )

            // Operation/document/hash constants resolve and match the request factory inputs.
            val requestMethod = operationClass.getMethod("request", Int::class.javaPrimitiveType)
            val request = requestMethod.invoke(operationClass.getDeclaredField("INSTANCE").get(null), 15)
            val queryField = request.javaClass.getMethod("getQuery").invoke(request) as String
            val operationNameField = request.javaClass.getMethod("getOperationName").invoke(request) as String
            assertEquals("query GetItems(\$first: Int) { items(first: \$first) { id } }", queryField)
            assertEquals("GetItems", operationNameField)
        }
    }
}
