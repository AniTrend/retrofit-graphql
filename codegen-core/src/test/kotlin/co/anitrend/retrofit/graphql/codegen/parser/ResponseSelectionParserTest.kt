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
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import co.anitrend.retrofit.graphql.codegen.schema.SchemaCompiler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class ResponseSelectionParserTest {

    private lateinit var githubIndex: SchemaIndex
    private lateinit var anilistIndex: SchemaIndex
    private lateinit var schemaCompiler: SchemaCompiler

    @Before
    fun setUp() {
        schemaCompiler = SchemaCompiler()

        val githubFile =
            fixtureFile(
                "fixtures/simple/schemas/github-simple.graphqls",
            )
        val githubTypes =
            SchemaParser().parseWithRootTypes(githubFile)
        githubIndex =
            SchemaIndex.from(
                types = githubTypes.types,
                queryTypeName = githubTypes.queryTypeName,
                mutationTypeName = githubTypes.mutationTypeName,
                subscriptionTypeName = githubTypes.subscriptionTypeName,
            )

        val anilistFile =
            fixtureFile("fixtures/schemas/anilist.graphqls")
        val anilistTypes =
            SchemaParser().parseWithRootTypes(anilistFile)
        anilistIndex =
            SchemaIndex.from(
                types = anilistTypes.types,
                queryTypeName = anilistTypes.queryTypeName,
                mutationTypeName = anilistTypes.mutationTypeName,
                subscriptionTypeName = anilistTypes.subscriptionTypeName,
            )
    }

    // --- Simple query ---

    @Test
    fun `parses simple query with scalar fields`() {
        val parser = ResponseSelectionParser(githubIndex)
        val operation = buildOperation(
            name = "GetUser",
            type = OperationType.QUERY,
            document = operationText("fixtures/simple/queries/GetUser.graphql"),
        )

        val result = parser.parse(operation)

        assertEquals("Query", result.parentType)
        assertEquals(1, result.fields.size)

        val viewer = result.fields.first()
        assertEquals("viewer", viewer.responseName)
        assertEquals("viewer", viewer.schemaName)
        assertEquals("User", resolveNamed(viewer.outputType))

        val userFields = viewer.selectionSet!!
        assertEquals("User", userFields.parentType)
        val fieldNames = userFields.fields.map { it.responseName }
        assertTrue(fieldNames.containsAll(listOf("id", "login", "name", "bio")))
    }

    // --- Mutation ---

    @Test
    fun `parses mutation with nested response`() {
        val parser = ResponseSelectionParser(githubIndex)
        val operation = buildOperation(
            name = "UpdateBio",
            type = OperationType.MUTATION,
            document = operationText(
                "fixtures/simple/mutations/UpdateBio.graphql",
            ),
        )

        val result = parser.parse(operation)

        assertEquals("Mutation", result.parentType)
        val updateBio = result.fields.first { it.responseName == "updateBio" }
        val nested = updateBio.selectionSet!!
        val user = nested.fields.first { it.responseName == "user" }
        val userFields = user.selectionSet!!
        val fieldNames = userFields.fields.map { it.responseName }
        assertTrue(fieldNames.containsAll(listOf("id", "bio")))
    }

    // --- Aliased fields ---

    @Test
    fun `parses aliased fields with correct response names`() {
        val parser = ResponseSelectionParser(anilistIndex)
        val operation = buildOperation(
            name = "AliasedFields",
            type = OperationType.QUERY,
            document = operationText(
                "fixtures/advanced/aliases/AliasedFields.graphql",
            ),
        )

        val result = parser.parse(operation)

        val media = result.fields.first { it.responseName == "Media" }
        val mediaFields = media.selectionSet!!

        val englishTitle = mediaFields.fields.find { it.responseName == "englishTitle" }
        assertNotNull("Should have aliased englishTitle", englishTitle)
        assertEquals("title", englishTitle!!.schemaName)
        assertEquals(
            "english",
            englishTitle.selectionSet!!.fields.first().responseName,
        )

        val nativeTitle = mediaFields.fields.find { it.responseName == "nativeTitle" }
        assertNotNull("Should have aliased nativeTitle", nativeTitle)
        assertEquals("title", nativeTitle!!.schemaName)
        assertEquals(
            "native",
            nativeTitle.selectionSet!!.fields.first().responseName,
        )
    }

    // --- Deterministic ordering ---

    @Test
    fun `fields are ordered by responseName`() {
        val parser = ResponseSelectionParser(githubIndex)
        val operation = buildOperation(
            name = "GetUser",
            type = OperationType.QUERY,
            document = operationText("fixtures/simple/queries/GetUser.graphql"),
        )

        val result = parser.parse(operation)
        val viewer = result.fields.first()
        val fieldNames = viewer.selectionSet!!.fields.map { it.responseName }
        assertEquals(fieldNames.sorted(), fieldNames)
    }

    // --- List types ---

    @Test
    fun `parses list field types`() {
        val parser = ResponseSelectionParser(anilistIndex)
        val document =
            """query TestQuery { Page { media { id } } }""".trimIndent()
        val operation = buildOperation(
            name = "TestQuery",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val page = result.fields.first { it.responseName == "Page" }
        val media = page.selectionSet!!.fields.first { it.responseName == "media" }
        assertTrue(media.outputType is GraphQLType.List)
    }

    // --- Schema validation ---

    @Test
    fun `valid operation passes schema validation`() {
        val schema = schemaCompiler.compile(
            fixtureFile(
                "fixtures/simple/schemas/github-simple.graphqls",
            ).readText(),
        )
        val result = schemaCompiler.validate(
            schema,
            operationText("fixtures/simple/queries/GetUser.graphql"),
        )
        assertTrue("Should be valid: ${result.errors}", result.isValid)
    }

    // --- Error: invalid field ---

    @Test(expected = IllegalArgumentException::class)
    fun `invalid field throws path-aware error`() {
        val parser = ResponseSelectionParser(githubIndex)
        val document = """query BadQuery { viewer { nonexistent } }"""
        val operation = buildOperation(
            name = "BadQuery",
            type = OperationType.QUERY,
            document = document,
        )

        parser.parse(operation)
    }

    // --- Error: missing root type ---

    @Test(expected = IllegalArgumentException::class)
    fun `missing mutation root type throws`() {
        // github-simple schema has no subscription type
        val parser = ResponseSelectionParser(githubIndex)
        val operation = buildOperation(
            name = "BadSubscription",
            type = OperationType.SUBSCRIPTION,
            document = operationText("fixtures/simple/queries/GetUser.graphql"),
        )

        parser.parse(operation)
    }

    // --- Field merge ---

    @Test
    fun `repeated compatible fields are merged`() {
        val parser = ResponseSelectionParser(githubIndex)
        val document =
            """
            query TestMerge {
              viewer {
                id
                login
              }
              viewer {
                name
                bio
              }
            }
            """.trimIndent()
        val operation = buildOperation(
            name = "TestMerge",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val viewer = result.fields.first { it.responseName == "viewer" }
        val fieldNames = viewer.selectionSet!!.fields.map { it.responseName }
        assertEquals(4, fieldNames.size)
        assertTrue(fieldNames.containsAll(listOf("id", "login", "name", "bio")))
    }

    // --- Nested fragments ---

    @Test
    fun `parses nested fragment selections`() {
        val parser = ResponseSelectionParser(anilistIndex)
        val operation = buildOperation(
            name = "GetMediaDetail",
            type = OperationType.QUERY,
            document = operationText(
                "fixtures/advanced/fragments/MediaDetail.graphql",
            ),
        )

        val result = parser.parse(operation)

        val media = result.fields.first { it.responseName == "Media" }
        val mediaFields = media.selectionSet!!

        // MediaCore fields
        assertNotNull(mediaFields.fields.find { it.responseName == "id" })
        val title = mediaFields.fields.find { it.responseName == "title" }
        assertNotNull(title)
        val coverImage = mediaFields.fields.find { it.responseName == "coverImage" }
        assertNotNull(coverImage)

        // MediaExtended fields
        assertNotNull(mediaFields.fields.find { it.responseName == "description" })
        assertNotNull(mediaFields.fields.find { it.responseName == "genres" })
    }

    // --- __typename injection for abstract types ---

    @Test
    fun `auto-injects __typename into abstract type selections`() {
        val parser = ResponseSelectionParser(githubIndex)
        val document =
            """
            query NodeQuery {
              node(id: "123") {
                id
                ... on User {
                  login
                  name
                }
              }
            }
            """.trimIndent()
        val operation = buildOperation(
            name = "NodeQuery",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val node = result.fields.first { it.responseName == "node" }
        val nodeFields = node.selectionSet!!

        // Should have __typename auto-injected
        val typename = nodeFields.fields.find { it.schemaName == "__typename" }
        assertNotNull("Should have __typename field", typename)
        assertTrue(
            "__typename should be first field",
            nodeFields.fields.first().schemaName == "__typename",
        )
    }

    // --- Union type with applicableTypes scoping ---

    @Test
    fun `union inline fragments tag fields with applicableTypes`() {
        val parser = ResponseSelectionParser(anilistIndex)
        val document =
            """
            query ActivityQuery {
              Page {
                activities {
                  ... on ListActivity { status progress }
                  ... on TextActivity { text }
                }
              }
            }
            """.trimIndent()
        val operation = buildOperation(
            name = "ActivityQuery",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val page = result.fields.first { it.responseName == "Page" }
        val activities = page.selectionSet!!.fields.first { it.responseName == "activities" }
        val unionFields = activities.selectionSet!!

        val statusField = unionFields.fields.find { it.responseName == "status" }!!
        val textField = unionFields.fields.find { it.responseName == "text" }!!
        val typenameField = unionFields.fields.find { it.responseName == "__typename" }!!

        // status should be scoped to ListActivity
        assertEquals(setOf("ListActivity"), statusField.applicableTypes)

        // text should be scoped to TextActivity
        assertEquals(setOf("TextActivity"), textField.applicableTypes)

        // __typename should apply to all (empty applicableTypes)
        assertTrue(
            "__typename should apply to all types",
            typenameField.applicableTypes.isEmpty(),
        )
    }

    // --- Response identity tracking ---

    @Test
    fun `tracks response identity for aliased fields`() {
        val parser = ResponseSelectionParser(anilistIndex)
        val operation = buildOperation(
            name = "AliasedFields",
            type = OperationType.QUERY,
            document = operationText(
                "fixtures/advanced/aliases/AliasedFields.graphql",
            ),
        )

        val result = parser.parse(operation)
        val media = result.fields.first { it.responseName == "Media" }
        val mediaFields = media.selectionSet!!

        val englishTitle = mediaFields.fields.find { it.responseName == "englishTitle" }!!
        val nativeTitle = mediaFields.fields.find { it.responseName == "nativeTitle" }!!

        // Each alias should have a distinct responseIdentity
        assertEquals(
            "MediaEnglishTitle",
            englishTitle.selectionSet!!.responseIdentity,
        )
        assertEquals(
            "MediaNativeTitle",
            nativeTitle.selectionSet!!.responseIdentity,
        )
    }

    // --- Explicit __typename ---

    @Test
    fun `explicit __typename selection does not crash`() {
        val parser = ResponseSelectionParser(githubIndex)
        val document =
            """
            query TypenameQuery {
              viewer {
                __typename
                id
                login
              }
            }
            """.trimIndent()
        val operation = buildOperation(
            name = "TypenameQuery",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val viewer = result.fields.first { it.responseName == "viewer" }

        // __typename should be present as a field
        val typename = viewer.selectionSet!!.fields.find { it.responseName == "__typename" }
        assertNotNull("Should have __typename field", typename)
    }

    // --- Union __typename injection ---

    @Test
    fun `union type selections get auto-injected __typename`() {
        val parser = ResponseSelectionParser(anilistIndex)
        val document =
            """
            query ActivityQuery {
              Page {
                activities {
                  ... on ListActivity { status }
                  ... on TextActivity { text }
                }
              }
            }
            """.trimIndent()
        val operation = buildOperation(
            name = "ActivityQuery",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val page = result.fields.first { it.responseName == "Page" }
        val activities = page.selectionSet!!.fields.first { it.responseName == "activities" }

        // activities field has type ActivityUnion, so __typename should be injected
        val unionFields = activities.selectionSet!!
        assertTrue(
            "Should have __typename field",
            unionFields.fields.any { it.schemaName == "__typename" },
        )
    }

    // --- Unaliased __typename for discriminator injection ---

    @Test
    fun `aliased __typename does not suppress auto-injection`() {
        val parser = ResponseSelectionParser(githubIndex)
        val document =
            """
            query NodeQuery {
              node(id: "123") {
                kind: __typename
                id
                ... on User {
                  login
                }
              }
            }
            """.trimIndent()
        val operation = buildOperation(
            name = "NodeQuery",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val node = result.fields.first { it.responseName == "node" }
        val nodeFields = node.selectionSet!!

        // Aliased kind: __typename should be present
        val kindField = nodeFields.fields.find { it.responseName == "kind" }
        assertNotNull("Should have aliased kind field", kindField)
        assertEquals("__typename", kindField!!.schemaName)

        // Unaliased __typename should still be auto-injected
        val typenameField = nodeFields.fields.find { it.responseName == "__typename" }
        assertNotNull("Should have auto-injected __typename despite alias", typenameField)
    }

    @Test
    fun `normal __typename suppresses auto-injection`() {
        val parser = ResponseSelectionParser(githubIndex)
        val document =
            """
            query NodeQuery {
              node(id: "123") {
                __typename
                id
                ... on User {
                  login
                }
              }
            }
            """.trimIndent()
        val operation = buildOperation(
            name = "NodeQuery",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val node = result.fields.first { it.responseName == "node" }
        val nodeFields = node.selectionSet!!

        // Should have exactly one __typename field
        val typenameFields = nodeFields.fields.filter { it.schemaName == "__typename" }
        assertEquals(
            "Should have exactly one __typename field",
            1,
            typenameFields.size,
        )
        assertEquals(
            "The __typename field should have responseName __typename",
            "__typename",
            typenameFields.first().responseName,
        )
    }

    // --- Fragment type conditions on interfaces and unions ---

    @Test
    fun `fragment spread on interface resolves applicableTypes to concrete types`() {
        val parser = ResponseSelectionParser(githubIndex)
        // Fragment spread on Node interface (not a concrete object)
        val document =
            """
            query NodeFragmentQuery {
              node(id: "123") {
                ... on Node {
                  id
                }
                ... on User {
                  login
                  name
                }
              }
            }
            """.trimIndent()
        val operation = buildOperation(
            name = "NodeFragmentQuery",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val node = result.fields.first { it.responseName == "node" }
        val nodeFields = node.selectionSet!!

        // The `id` field from the Node interface fragment should have
        // applicableTypes = {"User"} (not {"Node"}), since only User
        // implements Node in this schema.
        val idField = nodeFields.fields.find { it.responseName == "id" }!!
        assertTrue(
            "id should be applicable to User (not Node itself)",
            idField.applicableTypes.contains("User"),
        )
        assertFalse(
            "id should NOT have Node in applicableTypes",
            idField.applicableTypes.contains("Node"),
        )
    }

    @Test
    fun `inline fragment on union has applicableTypes with concrete member types`() {
        val parser = ResponseSelectionParser(anilistIndex)
        val document =
            """
            query ActivityQuery {
              Page {
                activities {
                  ... on ListActivity { status progress }
                  ... on TextActivity { text }
                }
              }
            }
            """.trimIndent()
        val operation = buildOperation(
            name = "ActivityQuery",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val page = result.fields.first { it.responseName == "Page" }
        val activities = page.selectionSet!!.fields.first { it.responseName == "activities" }
        val unionFields = activities.selectionSet!!

        val statusField = unionFields.fields.find { it.responseName == "status" }!!
        val textField = unionFields.fields.find { it.responseName == "text" }!!

        // For object types like ListActivity, possibleTypesFor returns empty,
        // so applicableTypes falls back to just the type condition name.
        assertEquals(setOf("ListActivity"), statusField.applicableTypes)
        assertEquals(setOf("TextActivity"), textField.applicableTypes)
    }

    // --- Helpers ---

    private fun fixtureFile(path: String): File {
        val url =
            checkNotNull(
                this::class.java.classLoader.getResource(path),
            ) { "Fixture not found: $path" }
        return File(url.toURI())
    }

    private fun operationText(path: String): String {
        return fixtureFile(path).readText()
    }

    private fun buildOperation(
        name: String,
        type: OperationType,
        document: String,
    ): GraphQLOperationInfo {
        return GraphQLOperationInfo(
            name = name,
            type = type,
            document = document,
            sourceFile = "$name.graphql",
            variables = emptyList(),
        )
    }

    private fun resolveNamed(type: GraphQLType): String {
        return when (type) {
            is GraphQLType.Named -> type.name
            is GraphQLType.List -> resolveNamed(type.of)
        }
    }
}
