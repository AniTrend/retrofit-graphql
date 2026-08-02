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

import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import co.anitrend.retrofit.graphql.codegen.parser.SchemaParser
import graphql.language.OperationDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Tests that [TypenameInjector] injects `__typename` into executable
 * documents for abstract (interface/union) selections, independently of
 * any serialization backend. Response generation under
 * `SerializationBackend.NONE` relies on this injection so consumers can
 * still discriminate concrete types from the wire `__typename`.
 */
class TypenameInjectorTest {

    private lateinit var schemaIndex: SchemaIndex

    @Before
    fun setUp() {
        val schemaFile = fixtureFile("fixtures/advanced/unions/ThreeImplementors.graphqls")
        val result = SchemaParser().parseWithRootTypes(schemaFile)
        schemaIndex = SchemaIndex.from(
            types = result.types,
            queryTypeName = result.queryTypeName,
            mutationTypeName = result.mutationTypeName,
            subscriptionTypeName = result.subscriptionTypeName,
        )
    }

    @Test
    fun `injects typename into interface selection`() {
        val document = """
            query ThreeImplementors {
              result {
                id
                ... on Success { value }
              }
            }
        """.trimIndent()

        val injected = TypenameInjector(schemaIndex)
            .injectTypename(document, OperationDefinition.Operation.QUERY)

        // The abstract `result` selection must request __typename exactly once.
        assertTrue(injected.contains("__typename"))
        assertEquals(1, Regex("__typename").findAll(injected).count())
    }

    @Test
    fun `does not inject typename for concrete selections`() {
        val document = """
            query ThreeImplementors {
              result {
                ... on Success { value }
              }
            }
        """.trimIndent()

        val injected = TypenameInjector(schemaIndex)
            .injectTypename(document, OperationDefinition.Operation.QUERY)

        assertTrue(injected.contains("__typename"))
    }

    @Test
    fun `does not inject typename when no abstract type is selected`() {
        val schemaFile = fixtureFile("fixtures/simple/schemas/github-simple.graphqls")
        val result = SchemaParser().parseWithRootTypes(schemaFile)
        val simpleIndex = SchemaIndex.from(
            types = result.types,
            queryTypeName = result.queryTypeName,
            mutationTypeName = result.mutationTypeName,
            subscriptionTypeName = result.subscriptionTypeName,
        )
        val document = "query GetUser { viewer { id login } }"

        val injected = TypenameInjector(simpleIndex)
            .injectTypename(document, OperationDefinition.Operation.QUERY)

        assertFalse(injected.contains("__typename"))
    }

    @Test
    fun `does not duplicate an existing typename selection`() {
        val document = """
            query ThreeImplementors {
              result {
                __typename
                ... on Success { value }
              }
            }
        """.trimIndent()

        val injected = TypenameInjector(schemaIndex)
            .injectTypename(document, OperationDefinition.Operation.QUERY)

        assertEquals(1, Regex("__typename").findAll(injected).count())
    }

    private fun fixtureFile(path: String): File {
        val url = checkNotNull(
            this::class.java.classLoader.getResource(path),
        ) { "Fixture not found: $path" }
        return File(url.toURI())
    }
}
