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
import co.anitrend.retrofit.graphql.codegen.model.RuntimePath
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import co.anitrend.retrofit.graphql.codegen.model.SchemaType
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

    // --- Union type with runtimePaths scoping ---

    @Test
    fun `union inline fragments tag fields with runtimePaths`() {
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

        // status should be scoped to ListActivity via runtimePaths
        assertTrue(
            "status should have runtimePaths for ListActivity",
            statusField.runtimePaths.any { rp ->
                rp.assignments.values.contains("ListActivity")
            },
        )

        // text should be scoped to TextActivity
        assertTrue(
            "text should have runtimePaths for TextActivity",
            textField.runtimePaths.any { rp ->
                rp.assignments.values.contains("TextActivity")
            },
        )

        // __typename should apply to all (empty runtimePaths)
        assertTrue(
            "__typename should apply to all types",
            typenameField.runtimePaths.isEmpty(),
        )
    }

    // --- Response path tracking ---

    @Test
    fun `tracks response path for aliased fields`() {
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

        // Each alias should have a distinct responsePath
        assertEquals(
            listOf("Media", "englishTitle"),
            englishTitle.selectionSet!!.responsePath,
        )
        assertEquals(
            listOf("Media", "nativeTitle"),
            nativeTitle.selectionSet!!.responsePath,
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
    fun `fragment spread on interface resolves runtimePaths to concrete types`() {
        val parser = ResponseSelectionParser(githubIndex)
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
        // runtimePaths containing "User" (not "Node")
        val idField = nodeFields.fields.find { it.responseName == "id" }!!
        val idConcreteTypes = idField.runtimePaths
            .flatMap { it.assignments.values }
            .toSet()
        assertTrue(
            "id should be applicable to User (not Node itself)",
            idConcreteTypes.contains("User"),
        )
        assertFalse(
            "id should NOT have Node in runtimePaths",
            idConcreteTypes.contains("Node"),
        )
    }

    @Test
    fun `inline fragment on union has runtimePaths with concrete member types`() {
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

        val statusTypes = statusField.runtimePaths
            .flatMap { it.assignments.values }
            .toSet()
        assertEquals(setOf("ListActivity"), statusTypes)

        val textTypes = textField.runtimePaths
            .flatMap { it.assignments.values }
            .toSet()
        assertEquals(setOf("TextActivity"), textTypes)
    }

    // --- Nested inline fragments compose runtimePaths ---

    @Test
    fun `nested inline fragments compose runtimePaths via Cartesian product`() {
        val parser = ResponseSelectionParser(githubIndex)
        val document = """
            query NestedNodeQuery {
              node(id: "123") {
                ... on Node {
                  id
                  ... on User {
                    login
                  }
                }
              }
            }
        """.trimIndent()
        val operation = buildOperation(
            name = "NestedNodeQuery",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val node = result.fields.first { it.responseName == "node" }
        val nodeFields = node.selectionSet!!

        // id comes from outer Node fragment
        val idField = nodeFields.fields.find { it.responseName == "id" }!!
        val idTypes = idField.runtimePaths
            .flatMap { it.assignments.values }
            .toSet()
        assertEquals(
            "id should have Node implementors as concrete types",
            setOf("User"),
            idTypes,
        )

        // login comes from nested User fragment
        val loginField = nodeFields.fields.find { it.responseName == "login" }!!
        val loginTypes = loginField.runtimePaths
            .flatMap { it.assignments.values }
            .toSet()
        assertEquals(
            "login should be scoped to User only",
            setOf("User"),
            loginTypes,
        )
    }

    // --- Fragment type condition on interface resolves fields from the interface ---

    @Test
    fun `fragment on interface resolves fields from the interface definition`() {
        val parser = ResponseSelectionParser(githubIndex)
        val document = """
            query InterfaceFragmentQuery {
              node(id: "123") {
                ... on Node {
                  id
                }
              }
            }
        """.trimIndent()
        val operation = buildOperation(
            name = "InterfaceFragmentQuery",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val node = result.fields.first { it.responseName == "node" }
        val nodeFields = node.selectionSet!!

        val idField = nodeFields.fields.find { it.responseName == "id" }
        assertNotNull("Should have id field resolved from Node interface", idField)
        assertEquals("id", idField!!.schemaName)

        val typename = nodeFields.fields.find { it.schemaName == "__typename" }
        assertNotNull("Should have __typename auto-injected for abstract node", typename)
    }

    // --- Declared type used as parentType ---

    @Test
    fun `interface-returning field uses declared type as parentType not first concrete type`() {
        val parser = ResponseSelectionParser(githubIndex)
        val document = """
            query InterfaceParentQuery {
              node(id: "123") {
                id
              }
            }
        """.trimIndent()
        val operation = buildOperation(
            name = "InterfaceParentQuery",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val node = result.fields.first { it.responseName == "node" }
        val nodeFields = node.selectionSet!!

        assertEquals(
            "parentType should be the declared interface name",
            "Node",
            nodeFields.parentType,
        )
    }

    @Test
    fun `union-returning field uses declared type as parentType not first concrete type`() {
        val parser = ResponseSelectionParser(anilistIndex)
        val document = """
            query UnionParentQuery {
              Page {
                activities {
                  ... on ListActivity { status }
                  ... on TextActivity { text }
                }
              }
            }
        """.trimIndent()
        val operation = buildOperation(
            name = "UnionParentQuery",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val page = result.fields.first { it.responseName == "Page" }
        val activities = page.selectionSet!!.fields.first { it.responseName == "activities" }
        val unionFields = activities.selectionSet!!

        assertEquals(
            "parentType should be the declared union name",
            "ActivityUnion",
            unionFields.parentType,
        )
    }

    // --- Response path collision safety ---

    @Test
    fun `response paths use list to prevent collisions`() {
        val parser = ResponseSelectionParser(githubIndex)
        val document = """
            query FooBarFlat {
              viewer {
                login
                name
              }
            }
        """.trimIndent()
        val operation = buildOperation(
            name = "FooBarFlat",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val viewer = result.fields.first { it.responseName == "viewer" }

        assertEquals(listOf("viewer"), viewer.selectionSet!!.responsePath)
    }

    @Test
    fun `nested object path has correct response path segments`() {
        val parser = ResponseSelectionParser(githubIndex)
        val document = operationText(
            "fixtures/simple/mutations/UpdateBio.graphql",
        )
        val operation = buildOperation(
            name = "UpdateBio",
            type = OperationType.MUTATION,
            document = document,
        )

        val result = parser.parse(operation)
        val updateBio = result.fields.first { it.responseName == "updateBio" }

        assertEquals(listOf("updateBio"), updateBio.selectionSet!!.responsePath)

        val user = updateBio.selectionSet!!.fields.first { it.responseName == "user" }
        assertEquals(
            listOf("updateBio", "user"),
            user.selectionSet!!.responsePath,
        )
    }

    // --- Inline fragments without type conditions ---

    @Test
    fun `inline fragment without type condition uses parent type and has no additional runtimePaths`() {
        val parser = ResponseSelectionParser(githubIndex)
        val document = """
            query ConditionalFragmentQuery(${'$'}includeName: Boolean!) {
              viewer {
                ... @include(if: ${'$'}includeName) {
                  name
                  bio
                }
              }
            }
        """.trimIndent()
        val operation = buildOperation(
            name = "ConditionalFragmentQuery",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val viewer = result.fields.first { it.responseName == "viewer" }
        val viewerFields = viewer.selectionSet!!

        // name should be present and conditional
        val nameField = viewerFields.fields.find { it.responseName == "name" }
        assertNotNull("Should have name field", nameField)
        assertTrue("name should be conditional", nameField!!.condition.mayBeAbsent)

        // bio should be present and conditional
        val bioField = viewerFields.fields.find { it.responseName == "bio" }
        assertNotNull("Should have bio field", bioField)
        assertTrue("bio should be conditional", bioField!!.condition.mayBeAbsent)

        // runtimePaths should be empty (no type condition = no scope)
        assertTrue(
            "name runtimePaths should be empty",
            nameField.runtimePaths.isEmpty(),
        )
        assertTrue(
            "bio runtimePaths should be empty",
            bioField.runtimePaths.isEmpty(),
        )
    }

    // --- Nested abstract types preserve local runtimePaths ---

    @Test
    fun `nested inline fragments preserve local runtimePaths for inner abstract type`() {
        val parser = ResponseSelectionParser(githubIndex)
        val document = """
            query NestedNodeQuery2 {
              node(id: "123") {
                ... on Node {
                  id
                  ... on User {
                    login
                    name
                  }
                }
              }
            }
        """.trimIndent()
        val operation = buildOperation(
            name = "NestedNodeQuery2",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val node = result.fields.first { it.responseName == "node" }
        val nodeFields = node.selectionSet!!

        val loginField = nodeFields.fields.find { it.responseName == "login" }!!
        val loginTypes = loginField.runtimePaths
            .flatMap { it.assignments.values }
            .toSet()
        assertEquals(
            "login should be scoped to User",
            setOf("User"),
            loginTypes,
        )

        val idField = nodeFields.fields.find { it.responseName == "id" }!!
        val idTypes = idField.runtimePaths
            .flatMap { it.assignments.values }
            .toSet()
        assertEquals(
            "id should have Node implementors as concrete types",
            setOf("User"),
            idTypes,
        )
    }

    // ============================================================
    // Test A: common interface field (parser)
    // ============================================================

    @Test
    fun `testA common interface field parsed as separate variants`() {
        val schemaFile = fixtureFile(
            "fixtures/advanced/unions/ThreeImplementors.graphqls",
        )
        val schemaResult = SchemaParser().parseWithRootTypes(schemaFile)
        val schemaIndex = SchemaIndex.from(
            types = schemaResult.types,
            queryTypeName = schemaResult.queryTypeName,
            mutationTypeName = schemaResult.mutationTypeName,
            subscriptionTypeName = schemaResult.subscriptionTypeName,
        )

        val parser = ResponseSelectionParser(schemaIndex)
        val operation = buildOperation(
            name = "ThreeImplementors",
            type = OperationType.QUERY,
            document = operationText(
                "fixtures/advanced/unions/ThreeImplementors.graphql",
            ),
        )

        val result = parser.parse(operation)
        val resultField = result.fields.first { it.responseName == "result" }
        val resultFields = resultField.selectionSet!!

        // detail field should have variants from Success and Failure fragments
        val detailVariants = resultFields.fields.filter { it.responseName == "detail" }
        assertTrue(
            "Should have multiple detail field variants",
            detailVariants.size >= 2,
        )

        // Verify that value and reason fields are present in the nested
        // selections appropriately
        val allNestedFields = detailVariants
            .mapNotNull { it.selectionSet }
            .flatMap { it.fields }
        val nestedResponseNames = allNestedFields.map { it.responseName }.toSet()
        assertTrue("Should have id field", "id" in nestedResponseNames)
        assertTrue("Should have value field", "value" in nestedResponseNames)
        assertTrue("Should have reason field", "reason" in nestedResponseNames)
    }

    // ============================================================
    // Test B: nested abstract paths (parser)
    // ============================================================

    @Test
    fun `testB nested abstract paths parsed with composed runtime paths`() {
        val schemaFile = fixtureFile(
            "fixtures/advanced/unions/NestedAbstractBranches.graphqls",
        )
        val schemaResult = SchemaParser().parseWithRootTypes(schemaFile)
        val schemaIndex = SchemaIndex.from(
            types = schemaResult.types,
            queryTypeName = schemaResult.queryTypeName,
            mutationTypeName = schemaResult.mutationTypeName,
            subscriptionTypeName = schemaResult.subscriptionTypeName,
        )

        val parser = ResponseSelectionParser(schemaIndex)
        val operation = buildOperation(
            name = "NestedAbstractBranches",
            type = OperationType.QUERY,
            document = operationText(
                "fixtures/advanced/unions/NestedAbstractBranches.graphql",
            ),
        )

        val result = parser.parse(operation)
        val outerField = result.fields.first { it.responseName == "outer" }
        val outerFields = outerField.selectionSet!!

        // inner field should be present (multiple variants from separate fragments)
        val innerFields = outerFields.fields.filter { it.responseName == "inner" }
        assertTrue(
            "Should have inner field variants",
            innerFields.isNotEmpty(),
        )

        // Collect detail field variants from ALL inner field variants
        val allDetailFields = innerFields
            .mapNotNull { it.selectionSet }
            .flatMap { it.fields }
            .filter { it.responseName == "detail" }

        // Each detail variant should have distinct runtime paths
        // for OuterA vs OuterB
        val detailRuntimeTypes = allDetailFields
            .flatMap { it.runtimePaths }
            .flatMap { it.assignments.values }
            .toSet()
        assertTrue(
            "Should have OuterA-related runtime types",
            "OuterA" in detailRuntimeTypes,
        )
        assertTrue(
            "Should have OuterB-related runtime types",
            "OuterB" in detailRuntimeTypes,
        )
    }

    // ============================================================
    // Test C: alias alternatives (parser)
    // ============================================================

    @Test
    fun `testC alias alternatives produces separate variants not alternatives list`() {
        val schemaFile = fixtureFile(
            "fixtures/advanced/unions/AliasAlternatives.graphqls",
        )
        val schemaResult = SchemaParser().parseWithRootTypes(schemaFile)
        val schemaIndex = SchemaIndex.from(
            types = schemaResult.types,
            queryTypeName = schemaResult.queryTypeName,
            mutationTypeName = schemaResult.mutationTypeName,
            subscriptionTypeName = schemaResult.subscriptionTypeName,
        )

        val parser = ResponseSelectionParser(schemaIndex)
        val operation = buildOperation(
            name = "SearchQuery",
            type = OperationType.QUERY,
            document = operationText(
                "fixtures/advanced/unions/AliasAlternatives.graphql",
            ),
        )

        val result = parser.parse(operation)
        val search = result.fields.first { it.responseName == "search" }
        val searchFields = search.selectionSet!!

        // There should be two separate field variants for "value"
        val valueVariants = searchFields.fields.filter { it.responseName == "value" }
        assertEquals(
            "Should have TWO separate field variants for 'value'",
            2,
            valueVariants.size,
        )

        val schemaNames = valueVariants.map { it.schemaName }.toSet()
        assertTrue("Should have displayName schema name", "displayName" in schemaNames)
        assertTrue("Should have repositoryName schema name", "repositoryName" in schemaNames)

        // Each variant should have its own runtime path set
        val userVariant = valueVariants.find { it.schemaName == "displayName" }!!
        assertTrue(
            "User variant should have User in runtime paths",
            userVariant.runtimePaths.any { rp -> rp.assignments.values.contains("User") },
        )

        val repoVariant = valueVariants.find { it.schemaName == "repositoryName" }!!
        assertTrue(
            "Repo variant should have Repository in runtime paths",
            repoVariant.runtimePaths.any { rp -> rp.assignments.values.contains("Repository") },
        )
    }

    // ============================================================
    // Test D: covariant object return (parser)
    // ============================================================

    @Test
    fun `testD covariant object return parsed correctly`() {
        val schemaFile = fixtureFile(
            "fixtures/advanced/unions/CovariantAnimals.graphqls",
        )
        val schemaResult = SchemaParser().parseWithRootTypes(schemaFile)
        val schemaIndex = SchemaIndex.from(
            types = schemaResult.types,
            queryTypeName = schemaResult.queryTypeName,
            mutationTypeName = schemaResult.mutationTypeName,
            subscriptionTypeName = schemaResult.subscriptionTypeName,
        )

        val parser = ResponseSelectionParser(schemaIndex)
        val operation = buildOperation(
            name = "Animals",
            type = OperationType.QUERY,
            document = operationText(
                "fixtures/advanced/unions/CovariantAnimals.graphql",
            ),
        )

        val result = parser.parse(operation)
        val animals = result.fields.first { it.responseName == "animals" }
        val animalsFields = animals.selectionSet!!

        // The friend field should have variants from Dog and Cat fragments
        val friendVariants = animalsFields.fields.filter { it.responseName == "friend" }

        // Each variant should have runtime paths scoped to the appropriate
        // concrete type
        val dogVariant = friendVariants.find {
            it.runtimePaths.any { rp -> rp.assignments.values.contains("Dog") }
        }
        assertNotNull("Should have Dog-scoped friend variant", dogVariant)

        val catVariant = friendVariants.find {
            it.runtimePaths.any { rp -> rp.assignments.values.contains("Cat") }
        }
        assertNotNull("Should have Cat-scoped friend variant", catVariant)

        // Dog friend should have barkVolume in nested selection
        val dogFriendFields = dogVariant!!.selectionSet?.fields?.map { it.responseName } ?: emptyList()
        assertTrue("Dog friend should have barkVolume", "barkVolume" in dogFriendFields)

        // Cat friend should have livesRemaining in nested selection
        val catFriendFields = catVariant!!.selectionSet?.fields?.map { it.responseName } ?: emptyList()
        assertTrue("Cat friend should have livesRemaining", "livesRemaining" in catFriendFields)
    }

    // ============================================================
    // Test E: directive composition
    // ============================================================

    @Test
    fun `testE directive composition case1 excluded by include false skip true`() {
        val parser = ResponseSelectionParser(githubIndex)
        val document = """
            query DirectiveTest1 {
              viewer {
                id
                name @include(if: true) @skip(if: true)
                login
              }
            }
        """.trimIndent()
        val operation = buildOperation(
            name = "DirectiveTest1",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val viewer = result.fields.first { it.responseName == "viewer" }
        val viewerFields = viewer.selectionSet!!

        // name should NOT be present (@skip(if: true) excludes)
        val nameField = viewerFields.fields.find { it.responseName == "name" }
        assertNull("name should be excluded when @skip(if: true)", nameField)

        // id and login should still be present
        assertNotNull("id should be present", viewerFields.fields.find { it.responseName == "id" })
        assertNotNull("login should be present", viewerFields.fields.find { it.responseName == "login" })
    }

    @Test
    fun `testE directive composition case2 include true skip variable yields mayBeAbsent`() {
        val parser = ResponseSelectionParser(githubIndex)
        val document = """
            query DirectiveTest2(${'$'}skip: Boolean!) {
              viewer {
                id
                name @include(if: true) @skip(if: ${'$'}skip)
                login
              }
            }
        """.trimIndent()
        val operation = buildOperation(
            name = "DirectiveTest2",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val viewer = result.fields.first { it.responseName == "viewer" }
        val viewerFields = viewer.selectionSet!!

        // name should be present with mayBeAbsent = true
        val nameField = viewerFields.fields.find { it.responseName == "name" }
        assertNotNull("name should be present with variable skip", nameField)
        assertTrue(
            "name should have mayBeAbsent = true",
            nameField!!.condition.mayBeAbsent,
        )
    }

    @Test
    fun `testE directive composition case3 include variable skip false yields mayBeAbsent`() {
        val parser = ResponseSelectionParser(githubIndex)
        val document = """
            query DirectiveTest3(${'$'}include: Boolean!) {
              viewer {
                id
                name @include(if: ${'$'}include) @skip(if: false)
                login
              }
            }
        """.trimIndent()
        val operation = buildOperation(
            name = "DirectiveTest3",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val viewer = result.fields.first { it.responseName == "viewer" }
        val viewerFields = viewer.selectionSet!!

        // name should be present with mayBeAbsent = true
        val nameField = viewerFields.fields.find { it.responseName == "name" }
        assertNotNull("name should be present with variable include", nameField)
        assertTrue(
            "name should have mayBeAbsent = true",
            nameField!!.condition.mayBeAbsent,
        )
    }

    @Test
    fun `testE directive composition case4 include variable skip variable yields mayBeAbsent`() {
        val parser = ResponseSelectionParser(githubIndex)
        val document = """
            query DirectiveTest4(${'$'}include: Boolean!, ${'$'}skip: Boolean!) {
              viewer {
                id
                name @include(if: ${'$'}include) @skip(if: ${'$'}skip)
                login
              }
            }
        """.trimIndent()
        val operation = buildOperation(
            name = "DirectiveTest4",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val viewer = result.fields.first { it.responseName == "viewer" }
        val viewerFields = viewer.selectionSet!!

        // name should be present with mayBeAbsent = true
        val nameField = viewerFields.fields.find { it.responseName == "name" }
        assertNotNull("name should be present with both variable directives", nameField)
        assertTrue(
            "name should have mayBeAbsent = true",
            nameField!!.condition.mayBeAbsent,
        )
    }

    // ============================================================
    // composeRuntimePaths unit tests
    // ============================================================

    @Test
    fun `composeRuntimePaths empty enclosing returns fragment unchanged`() {
        val enclosing = emptySet<RuntimePath>()
        val fragment = setOf(
            RuntimePath(mapOf(listOf("node") to "User")),
        )
        val result = ResponseSelectionParser.composeRuntimePaths(enclosing, fragment)
        assertEquals(fragment, result)
    }

    @Test
    fun `composeRuntimePaths empty fragment returns enclosing unchanged`() {
        val enclosing = setOf(
            RuntimePath(mapOf(listOf("node") to "User")),
        )
        val fragment = emptySet<RuntimePath>()
        val result = ResponseSelectionParser.composeRuntimePaths(enclosing, fragment)
        assertEquals(enclosing, result)
    }

    @Test
    fun `composeRuntimePaths Cartesian product with non-conflicting paths`() {
        val enclosing = setOf(
            RuntimePath(mapOf(listOf("outer") to "OuterA")),
        )
        val fragment = setOf(
            RuntimePath(mapOf(listOf("inner") to "InnerX")),
        )
        val result = ResponseSelectionParser.composeRuntimePaths(enclosing, fragment)
        assertEquals(1, result.size)
        val combined = result.first()
        assertEquals(
            mapOf(listOf("outer") to "OuterA", listOf("inner") to "InnerX"),
            combined.assignments,
        )
    }

    @Test
    fun `composeRuntimePaths discards conflicting assignments`() {
        val enclosing = setOf(
            RuntimePath(mapOf(listOf("node") to "User")),
        )
        val fragment = setOf(
            RuntimePath(mapOf(listOf("node") to "Post")),
        )
        val result = ResponseSelectionParser.composeRuntimePaths(enclosing, fragment)
        assertEquals(0, result.size)
    }

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
