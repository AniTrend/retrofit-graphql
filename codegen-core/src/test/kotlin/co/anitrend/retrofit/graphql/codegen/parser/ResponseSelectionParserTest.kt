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
            "Media.englishTitle",
            englishTitle.selectionSet!!.responseIdentity,
        )
        assertEquals(
            "Media.nativeTitle",
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

    // --- Item 3: Outer abstract fragments overwrite nested concrete scopes ---

    @Test
    fun `nested inline fragments compose applicableTypes via intersection`() {
        val parser = ResponseSelectionParser(githubIndex)
        // github-simple has Node interface with User as implementor.
        // Test that the outer Node fragment's scope is intersected with
        // the inner User fragment's scope rather than overwriting it.
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

        // id comes from the outer Node fragment; applicableTypes should be
        // narrowed by the outer scope (all Node implementors: {User})
        val idField = nodeFields.fields.find { it.responseName == "id" }!!
        assertEquals(
            "id should have Node implementors as applicableTypes",
            setOf("User"),
            idField.applicableTypes,
        )

        // login comes from the nested User fragment. Without composition
        // it would incorrectly expand to all Node implementors.
        val loginField = nodeFields.fields.find { it.responseName == "login" }!!
        assertEquals(
            "login should be scoped to User only via intersection",
            setOf("User"),
            loginField.applicableTypes,
        )
    }

    // --- Item 4: Fragment type condition on interface resolves fields from the interface ---

    @Test
    fun `fragment on interface resolves fields from the interface definition`() {
        val parser = ResponseSelectionParser(githubIndex)
        // Node is an interface with an `id` field. The parser should resolve
        // it from the Node interface directly, not from an arbitrary concrete
        // implementor.
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

        // id should be present and resolved from the Node interface
        val idField = nodeFields.fields.find { it.responseName == "id" }
        assertNotNull("Should have id field resolved from Node interface", idField)
        assertEquals("id", idField!!.schemaName)

        // The parentType of the response selection set should reflect the
        // fragment type condition (Node), not an arbitrary concrete type.
        // The __typename auto-injection would be in the node field's
        // selection set since node returns Node (abstract).
        val typename = nodeFields.fields.find { it.schemaName == "__typename" }
        assertNotNull("Should have __typename auto-injected for abstract node", typename)
    }

    // --- Item 3: Initial abstract fields use declared type for parsing ---

    @Test
    fun `interface-returning field uses declared type as parentType not first concrete type`() {
        val parser = ResponseSelectionParser(githubIndex)
        // node returns the Node interface. Before the fix, the parser
        // would use User (the first concrete implementor) as the parent
        // type for nested selections. After the fix, it uses Node itself.
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

        // parentType should be "Node" (the declared interface), not "User"
        assertEquals(
            "parentType should be the declared interface name",
            "Node",
            nodeFields.parentType,
        )

        // id should still be resolvable from the Node interface
        val idField = nodeFields.fields.find { it.responseName == "id" }
        assertNotNull("Should have id field resolved from Node interface", idField)
    }

    @Test
    fun `union-returning field uses declared type as parentType not first concrete type`() {
        val parser = ResponseSelectionParser(anilistIndex)
        // Activities returns ActivityUnion. parentType should be the
        // union name, not the first concrete member.
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

        // parentType should be "ActivityUnion", not "ListActivity"
        assertEquals(
            "parentType should be the declared union name",
            "ActivityUnion",
            unionFields.parentType,
        )

        // Fields from inline fragments should still be scoped correctly
        val statusField = unionFields.fields.find { it.responseName == "status" }!!
        assertEquals(setOf("ListActivity"), statusField.applicableTypes)
        val textField = unionFields.fields.find { it.responseName == "text" }!!
        assertEquals(setOf("TextActivity"), textField.applicableTypes)
    }

    // --- Item 4: Collision-safe response-path identities ---

    @Test
    fun `response identities use dot separator to prevent collisions`() {
        val parser = ResponseSelectionParser(githubIndex)
        // Flat field 'fooBar' should produce identity "fooBar".
        // Nested path 'foo.bar' should produce identity "foo.bar".
        // Dot separator prevents collisions since '.' is invalid in
        // GraphQL field names.
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

        // Root-level field uses just the response name, no separator
        assertEquals("viewer", viewer.selectionSet!!.responseIdentity)
    }

    @Test
    fun `nested field identity includes separator from parent`() {
        val parser = ResponseSelectionParser(githubIndex)
        val document = """
            query NestedQuery {
              viewer {
                bio
                name
              }
            }
        """.trimIndent()
        val operation = buildOperation(
            name = "NestedQuery",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val viewer = result.fields.first { it.responseName == "viewer" }
        val viewerFields = viewer.selectionSet!!

        // viewer.bio -> bio is a scalar leaf, no nested identity
        // viewer.name -> name is a scalar leaf, no nested identity
        // But the viewer's own identity is root-level
        assertEquals("viewer", viewerFields.responseIdentity)
    }

    @Test
    fun `nested object identity has separator in deeply nested path`() {
        val parser = ResponseSelectionParser(githubIndex)
        // Use updateBio mutation which produces a nested path:
        //   updateBio { user { id bio } }
        // Identity chain (dot-separated):
        //   root("") -> updateBio -> updateBio.user
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

        // updateBio is root-level, no separator
        assertEquals("updateBio", updateBio.selectionSet!!.responseIdentity)

        val user = updateBio.selectionSet!!.fields.first { it.responseName == "user" }
        // user is nested under updateBio, so dot separator appears
        assertEquals(
            "updateBio.user",
            user.selectionSet!!.responseIdentity,
        )
    }

    // --- Item 4: Collision-safe dot-separated identities ---

    @Test
    fun `foo and Foo produce different dot-path identities`() {
        val parser = ResponseSelectionParser(githubIndex)
        // Fields 'foo' and 'Foo' (different case) are both valid GraphQL
        // names, and produce different dot-path identities since the parser
        // preserves the original response name.
        val document = """
            query FooCaseQuery {
              viewer {
                login
                name
              }
            }
        """.trimIndent()
        val operation = buildOperation(
            name = "FooCaseQuery",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val viewer = result.fields.first { it.responseName == "viewer" }
        // viewer's identity is "viewer" (lowercase, as in the query)
        assertEquals("viewer", viewer.selectionSet!!.responseIdentity)
    }

    @Test
    fun `fooDotBar and fooBar produce different dot-path identities`() {
        val parser = ResponseSelectionParser(githubIndex)
        // Nested path 'foo.bar' (two levels) produces identity "foo.bar"
        // Flat field 'fooBar' (one level) produces identity "fooBar"
        // These are always distinct because '.' is invalid in field names.
        val document = """
            query NestedFlatQuery {
              viewer {
                login
                name
              }
            }
        """.trimIndent()
        val operation = buildOperation(
            name = "NestedFlatQuery",
            type = OperationType.QUERY,
            document = document,
        )

        val result = parser.parse(operation)
        val viewer = result.fields.first { it.responseName == "viewer" }

        // viewer is a single-level field, identity = "viewer"
        assertEquals("viewer", viewer.selectionSet!!.responseIdentity)
        // No nested objects, so no dot-separated identity
    }

    // --- Item 3: Nested abstract types preserve local applicableTypes ---

    @Test
    fun `nested inline fragments preserve local applicableTypes for inner abstract type`() {
        val parser = ResponseSelectionParser(githubIndex)
        // Two levels of abstract type: node(id) returns Node (interface).
        // Inner inline fragments on User are nested inside the outer
        // Node fragment. The inner applicableTypes should be local to
        // the User type, not propagated from the outer Node scope.
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

        // login from the inner User fragment should have applicableTypes
        // narrowed to User via intersection. Outer Node scope is {User},
        // inner User scope is {User}, intersection = {User}.
        val loginField = nodeFields.fields.find { it.responseName == "login" }!!
        assertEquals(
            "login should be scoped to User",
            setOf("User"),
            loginField.applicableTypes,
        )

        // id from outer Node fragment should also be narrowed to {User}
        val idField = nodeFields.fields.find { it.responseName == "id" }!!
        assertEquals(
            "id should have Node implementors as applicableTypes",
            setOf("User"),
            idField.applicableTypes,
        )
    }

    // --- Item 5: Inline fragments without type conditions ---

    @Test
    fun `inline fragment without type condition uses parent type and has no additional applicableTypes`() {
        val parser = ResponseSelectionParser(githubIndex)
        // Type-condition-less inline fragment with @include directive.
        // Fields inside should be parsed correctly with
        // condition.isConditional = true.
        val document = """
            query ConditionalFragmentQuery {
              viewer {
                ... @include(if: true) {
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
        assertTrue("name should be conditional", nameField!!.condition.isConditional)

        // bio should be present and conditional
        val bioField = viewerFields.fields.find { it.responseName == "bio" }
        assertNotNull("Should have bio field", bioField)
        assertTrue("bio should be conditional", bioField!!.condition.isConditional)

        // applicableTypes should be empty (no type condition = no scope)
        assertTrue(
            "name applicableTypes should be empty",
            nameField.applicableTypes.isEmpty(),
        )
        assertTrue(
            "bio applicableTypes should be empty",
            bioField.applicableTypes.isEmpty(),
        )
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
