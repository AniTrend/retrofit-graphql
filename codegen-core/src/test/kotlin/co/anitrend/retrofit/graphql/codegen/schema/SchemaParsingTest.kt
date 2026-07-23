package co.anitrend.retrofit.graphql.codegen.schema

import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.OutputField
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import co.anitrend.retrofit.graphql.codegen.model.SchemaType
import co.anitrend.retrofit.graphql.codegen.parser.SchemaParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SchemaParsingTest {

    private val parser = SchemaParser()

    // ---------------------------------------------------------------------------
    // Object type parsing
    // ---------------------------------------------------------------------------

    @Test
    fun `should parse object type with fields`() {
        val sdl =
            """
            type User {
                id: ID!
                name: String
                age: Int
            }
            """.trimIndent()

        val types = parse(sdl)

        val userType = types.filterIsInstance<SchemaType.ObjectType>().single()
        assertEquals("User", userType.name)
        assertEquals(3, userType.fields.size)
    }

    // ---------------------------------------------------------------------------
    // OutputField parsing
    // ---------------------------------------------------------------------------

    @Test
    fun `should parse output fields with correct types and nullability`() {
        val sdl =
            """
            type Profile {
                id: ID!
                bio: String
                score: Float
                active: Boolean
                avatar: Avatar
            }
            """.trimIndent()

        val types = parse(sdl)
        val profileType = types.filterIsInstance<SchemaType.ObjectType>().single()

        // id: ID! -> non-null named type
        val idField = profileType.fields.find { it.name == "id" }!!
        assertTrue(idField.type is GraphQLType.Named)
        assertEquals("ID", (idField.type as GraphQLType.Named).name)
        assertFalse((idField.type as GraphQLType.Named).nullable)

        // bio: String -> nullable named type
        val bioField = profileType.fields.find { it.name == "bio" }!!
        assertTrue(bioField.type is GraphQLType.Named)
        assertEquals("String", (bioField.type as GraphQLType.Named).name)
        assertTrue((bioField.type as GraphQLType.Named).nullable)

        // score: Float -> nullable named (maps)
        val scoreField = profileType.fields.find { it.name == "score" }!!
        assertEquals("Float", (scoreField.type as GraphQLType.Named).name)

        // active: Boolean -> nullable named
        val activeField = profileType.fields.find { it.name == "active" }!!
        assertEquals("Boolean", (activeField.type as GraphQLType.Named).name)

        // avatar: Avatar -> nullable named custom type
        val avatarField = profileType.fields.find { it.name == "avatar" }!!
        assertEquals("Avatar", (avatarField.type as GraphQLType.Named).name)
    }

    @Test
    fun `should parse output fields with list types`() {
        val sdl =
            """
            type Page {
                items: [String!]!
                users: [User]
            }
            """.trimIndent()

        val types = parse(sdl)
        val pageType = types.filterIsInstance<SchemaType.ObjectType>().single()

        // items: [String!]! -> non-null list of non-null String
        val itemsField = pageType.fields.find { it.name == "items" }!!
        assertTrue(itemsField.type is GraphQLType.List)
        val itemsList = itemsField.type as GraphQLType.List
        assertFalse(itemsList.nullable)
        assertTrue(itemsList.of is GraphQLType.Named)
        assertEquals("String", (itemsList.of as GraphQLType.Named).name)
        assertFalse((itemsList.of as GraphQLType.Named).nullable)

        // users: [User] -> nullable list of nullable User
        val usersField = pageType.fields.find { it.name == "users" }!!
        assertTrue(usersField.type is GraphQLType.List)
        val usersList = usersField.type as GraphQLType.List
        assertTrue(usersList.nullable)
        assertTrue(usersList.of is GraphQLType.Named)
        assertEquals("User", (usersList.of as GraphQLType.Named).name)
        assertTrue((usersList.of as GraphQLType.Named).nullable)
    }

    @Test
    fun `should parse output fields with arguments`() {
        val sdl =
            """
            type Query {
                user(id: ID!): User
                search(query: String!, limit: Int): [Result]
            }
            """.trimIndent()

        val types = parse(sdl)
        val queryType = types.filterIsInstance<SchemaType.ObjectType>().single()

        // user(id: ID!): User
        val userField = queryType.fields.find { it.name == "user" }!!
        assertEquals(1, userField.arguments.size)
        val userIdArg = userField.arguments.single()
        assertEquals("id", userIdArg.name)
        assertTrue(userIdArg.type is GraphQLType.Named)
        assertEquals("ID", (userIdArg.type as GraphQLType.Named).name)
        assertFalse((userIdArg.type as GraphQLType.Named).nullable)

        // search(query: String!, limit: Int): [Result]
        val searchField = queryType.fields.find { it.name == "search" }!!
        assertEquals(2, searchField.arguments.size)
        val queryArg = searchField.arguments.find { it.name == "query" }!!
        assertTrue(queryArg.type is GraphQLType.Named)
        assertEquals("String", (queryArg.type as GraphQLType.Named).name)
        assertFalse((queryArg.type as GraphQLType.Named).nullable)
        val limitArg = searchField.arguments.find { it.name == "limit" }!!
        assertEquals("Int", (limitArg.type as GraphQLType.Named).name)
        assertTrue((limitArg.type as GraphQLType.Named).nullable)
    }

    // ---------------------------------------------------------------------------
    // Interface parsing
    // ---------------------------------------------------------------------------

    @Test
    fun `should parse interface type with fields`() {
        val sdl =
            """
            interface Node {
                id: ID!
            }
            """.trimIndent()

        val types = parse(sdl)
        val nodeType = types.filterIsInstance<SchemaType.InterfaceType>().single()
        assertEquals("Node", nodeType.name)
        assertEquals(1, nodeType.fields.size)
        val idField = nodeType.fields.single()
        assertEquals("id", idField.name)
        assertTrue(idField.type is GraphQLType.Named)
        assertEquals("ID", (idField.type as GraphQLType.Named).name)
        assertFalse((idField.type as GraphQLType.Named).nullable)
    }

    // ---------------------------------------------------------------------------
    // Union parsing
    // ---------------------------------------------------------------------------

    @Test
    fun `should parse union type with member types`() {
        val sdl =
            """
            union SearchResult = User | Post
            """.trimIndent()

        val types = parse(sdl)
        val unionType = types.filterIsInstance<SchemaType.UnionType>().single()
        assertEquals("SearchResult", unionType.name)
        assertEquals(setOf("User", "Post"), unionType.memberTypes.toSet())
    }

    // ---------------------------------------------------------------------------
    // Object implements interfaces
    // ---------------------------------------------------------------------------

    @Test
    fun `should parse object type with implemented interfaces`() {
        val sdl =
            """
            interface Node { id: ID! }
            type User implements Node {
                id: ID!
                name: String
            }
            """.trimIndent()

        val types = parse(sdl)
        val userType = types.filterIsInstance<SchemaType.ObjectType>().single()
        assertEquals("User", userType.name)
        assertEquals(listOf("Node"), userType.interfaces)
    }

    // ---------------------------------------------------------------------------
    // SchemaIndex building
    // ---------------------------------------------------------------------------

    @Test
    fun `should build SchemaIndex with output types`() {
        val sdl =
            """
            interface Node { id: ID! }
            type User implements Node { id: ID! name: String }
            type Post { id: ID! title: String }
            union FeedItem = User | Post
            """.trimIndent()

        val types = parse(sdl)
        val index = SchemaIndex.from(types)

        assertEquals(2, index.objects.size)
        assertEquals(1, index.interfaces.size)
        assertEquals(1, index.unions.size)
    }

    @Test
    fun `possibleTypesFor should return implementing types for interfaces`() {
        val sdl =
            """
            interface Node { id: ID! }
            type User implements Node { id: ID! name: String }
            type Post implements Node { id: ID! title: String }
            type Comment { id: ID! body: String }
            """.trimIndent()

        val types = parse(sdl)
        val index = SchemaIndex.from(types)

        assertEquals(setOf("User", "Post"), index.possibleTypesFor("Node"))
        assertTrue(index.possibleTypesFor("NonExistent").isEmpty())
    }

    @Test
    fun `possibleTypesFor should return member types for unions`() {
        val sdl =
            """
            type User { name: String }
            type Post { title: String }
            union FeedItem = User | Post
            """.trimIndent()

        val types = parse(sdl)
        val index = SchemaIndex.from(types)

        assertEquals(setOf("User", "Post"), index.possibleTypesFor("FeedItem"))
    }

    @Test
    fun `isAbstractType should identify interfaces and unions`() {
        val sdl =
            """
            interface Node { id: ID! }
            type User { name: String }
            union Result = User
            """.trimIndent()

        val types = parse(sdl)
        val index = SchemaIndex.from(types)

        assertTrue(index.isAbstractType("Node"))
        assertTrue(index.isAbstractType("Result"))
        assertFalse(index.isAbstractType("User"))
        assertFalse(index.isAbstractType("NonExistent"))
    }

    @Test
    fun `objectType should return ObjectType by name`() {
        val sdl =
            """
            type User { name: String }
            """.trimIndent()

        val types = parse(sdl)
        val index = SchemaIndex.from(types)

        val user = index.objectType("User")
        assertNotNull(user)
        assertEquals("User", user!!.name)

        assertNull(index.objectType("NonExistent"))
    }

    // ---------------------------------------------------------------------------
    // Root type names
    // ---------------------------------------------------------------------------

    @Test
    fun `should extract root type names from SchemaDefinition`() {
        val sdl =
            """
            schema {
                query: MyQueryType
                mutation: MyMutationType
            }
            type MyQueryType { hello: String }
            type MyMutationType { setHello(msg: String!): String }
            """.trimIndent()

        val testFile = createTempFile(sdl)
        val result = parser.parseWithRootTypes(testFile)

        assertEquals("MyQueryType", result.queryTypeName)
        assertEquals("MyMutationType", result.mutationTypeName)
        assertNull(result.subscriptionTypeName)
    }

    @Test
    fun `should extract subscription type name from SchemaDefinition`() {
        val sdl =
            """
            schema {
                query: Query
                subscription: MySubscription
            }
            type Query { hello: String }
            type MySubscription { onEvent: String }
            """.trimIndent()

        val testFile = createTempFile(sdl)
        val result = parser.parseWithRootTypes(testFile)

        assertEquals("Query", result.queryTypeName)
        assertEquals("MySubscription", result.subscriptionTypeName)
        assertNull(result.mutationTypeName)
    }

    @Test
    fun `should use default root type names when no SchemaDefinition exists`() {
        val sdl =
            """
            type Query { hello: String }
            """.trimIndent()

        val testFile = createTempFile(sdl)
        val result = parser.parseWithRootTypes(testFile)

        assertEquals("Query", result.queryTypeName)
        assertNull(result.mutationTypeName)
        assertNull(result.subscriptionTypeName)
    }

    // ---------------------------------------------------------------------------
    // SchemaCompiler compile
    // ---------------------------------------------------------------------------

    @Test
    fun `should compile a valid GraphQL schema from minimal SDL`() {
        val compiler = SchemaCompiler()
        val sdl =
            """
            type Query {
                hello: String
            }
            """.trimIndent()

        val schema = compiler.compile(sdl)
        assertNotNull(schema)
        assertEquals("Query", schema.queryType.name)
    }

    @Test
    fun `should compile schema with custom scalars`() {
        val compiler = SchemaCompiler()
        val sdl =
            """
            scalar DateTime
            type Query {
                now: DateTime
            }
            """.trimIndent()

        val schema = compiler.compile(sdl)
        assertNotNull(schema)
    }

    // ---------------------------------------------------------------------------
    // SchemaCompiler validation
    // ---------------------------------------------------------------------------

    @Test
    fun `should validate a valid query against compiled schema`() {
        val compiler = SchemaCompiler()
        val schema =
            compiler.compile(
                """
                type Query {
                    hello: String
                }
                """.trimIndent(),
            )

        val result = compiler.validate(
            schema,
            """
            query TestQuery {
                hello
            }
            """.trimIndent(),
        )

        assertTrue("Expected valid query, got errors: ${result.errors}", result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `should return validation errors for invalid query`() {
        val compiler = SchemaCompiler()
        val schema =
            compiler.compile(
                """
                type Query {
                    hello: String
                }
                """.trimIndent(),
            )

        val result = compiler.validate(
            schema,
            """
            query TestQuery {
                nonexistentField
            }
            """.trimIndent(),
        )

        assertFalse("Expected invalid query", result.isValid)
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.errors.any { it.contains("nonexistentField") || it.contains("Can't find") })
    }

    @Test
    fun `should validate mutation against compiled schema`() {
        val compiler = SchemaCompiler()
        val schema =
            compiler.compile(
                """
                type Query {
                    _dummy: String
                }
                type Mutation {
                    setName(name: String!): String
                }
                """.trimIndent(),
            )

        val result = compiler.validate(
            schema,
            """
            mutation UpdateName {
                setName(name: "test")
            }
            """.trimIndent(),
        )

        assertTrue("Expected valid mutation, got errors: ${result.errors}", result.isValid)
    }

    // ---------------------------------------------------------------------------
    // Backward compatibility
    // ---------------------------------------------------------------------------

    @Test
    fun `should still parse InputObject types when output types are present`() {
        val sdl =
            """
            input CreateUserInput {
                name: String!
                email: String
            }
            type User {
                id: ID!
                name: String
            }
            """.trimIndent()

        val types = parse(sdl)

        val inputObjects = types.filterIsInstance<SchemaType.InputObject>()
        assertEquals(1, inputObjects.size)
        assertEquals("CreateUserInput", inputObjects.single().name)
        assertEquals(2, inputObjects.single().fields.size)

        val objectTypes = types.filterIsInstance<SchemaType.ObjectType>()
        assertEquals(1, objectTypes.size)
        assertEquals("User", objectTypes.single().name)
    }

    @Test
    fun `should still parse Enum types when output types are present`() {
        val sdl =
            """
            enum Status { OPEN CLOSED }
            type Issue {
                id: ID!
                status: Status
            }
            """.trimIndent()

        val types = parse(sdl)

        val enums = types.filterIsInstance<SchemaType.Enum>()
        assertEquals(1, enums.size)
        assertEquals("Status", enums.single().name)
        assertEquals(listOf("OPEN", "CLOSED"), enums.single().values)

        val objectTypes = types.filterIsInstance<SchemaType.ObjectType>()
        assertEquals(1, objectTypes.size)
    }

    @Test
    fun `should still parse Scalar types when output types are present`() {
        val sdl =
            """
            scalar DateTime
            type Event {
                id: ID!
                when: DateTime
            }
            """.trimIndent()

        val types = parse(sdl)

        val scalars = types.filterIsInstance<SchemaType.Scalar>()
        assertEquals(1, scalars.size)
        assertEquals("DateTime", scalars.single().name)

        val objectTypes = types.filterIsInstance<SchemaType.ObjectType>()
        assertEquals(1, objectTypes.size)
    }

    @Test
    fun `should parse simple github schema with all type kinds`() {
        val schemaFile = File("src/test/resources/fixtures/simple/schemas/github-simple.graphqls")
        val types = parser.parse(schemaFile)

        val objectTypes = types.filterIsInstance<SchemaType.ObjectType>()
        val interfaceTypes = types.filterIsInstance<SchemaType.InterfaceType>()
        val scalarTypes = types.filterIsInstance<SchemaType.Scalar>()

        val objectNames = objectTypes.map { it.name }.toSet()
        assertTrue("Query should be parsed", "Query" in objectNames)
        assertTrue("Mutation should be parsed", "Mutation" in objectNames)
        assertTrue("User should be parsed", "User" in objectNames)
        assertTrue("UpdateBioPayload should be parsed", "UpdateBioPayload" in objectNames)

        assertEquals(1, interfaceTypes.size)
        assertEquals("Node", interfaceTypes.single().name)

        // User implements Node
        val userType = objectTypes.find { it.name == "User" }!!
        assertEquals(listOf("Node"), userType.interfaces)

        // Query has node(id: ID!): Node
        val queryType = objectTypes.find { it.name == "Query" }!!
        val nodeField = queryType.fields.find { it.name == "node" }!!
        assertEquals(1, nodeField.arguments.size)
        assertEquals("id", nodeField.arguments.single().name)
        assertEquals("Node", (nodeField.type as GraphQLType.Named).name)

        // Mutation has updateBio(input: String!): UpdateBioPayload
        val mutationType = objectTypes.find { it.name == "Mutation" }!!
        val updateBioField = mutationType.fields.find { it.name == "updateBio" }!!
        assertEquals(1, updateBioField.arguments.size)
        assertEquals("UpdateBioPayload", (updateBioField.type as GraphQLType.Named).name)

        // No scalars should be in the fixture (standard scalars are built-in)
        assertTrue("No scalars expected in simple fixture", scalarTypes.isEmpty())
    }

    @Test
    fun `should parse anilist schema with unions`() {
        val schemaFile = File("src/test/resources/fixtures/schemas/anilist.graphqls")
        val types = parser.parse(schemaFile)

        val unionTypes = types.filterIsInstance<SchemaType.UnionType>()
        assertEquals(1, unionTypes.size)
        val activityUnion = unionTypes.single()
        assertEquals("ActivityUnion", activityUnion.name)
        assertEquals(setOf("ListActivity", "TextActivity"), activityUnion.memberTypes.toSet())

        val objectTypes = types.filterIsInstance<SchemaType.ObjectType>()
        val objectNames = objectTypes.map { it.name }.toSet()
        assertTrue("Query should be parsed", "Query" in objectNames)
        assertTrue("Media should be parsed", "Media" in objectNames)
        assertTrue("Page should be parsed", "Page" in objectNames)

        // Page.activities returns [ActivityUnion]
        val pageType = objectTypes.find { it.name == "Page" }!!
        val activitiesField = pageType.fields.find { it.name == "activities" }!!
        assertTrue(activitiesField.type is GraphQLType.List)
        assertEquals("ActivityUnion", ((activitiesField.type as GraphQLType.List).of as GraphQLType.Named).name)

        // Page.media has arguments
        val mediaField = pageType.fields.find { it.name == "media" }!!
        assertTrue(mediaField.arguments.isNotEmpty())
        assertEquals("id", mediaField.arguments.first().name)
    }

    @Test
    fun `SchemaIndex from single-parameter overload should still work`() {
        val types = listOf(
            SchemaType.InputObject("MyInput", emptyList()),
            SchemaType.Enum("Status", listOf("OPEN")),
            SchemaType.Scalar("DateTime"),
        )
        // This should compile without error
        val index = SchemaIndex.from(types)
        assertEquals(1, index.inputObjects.size)
        assertEquals(1, index.enums.size)
        assertEquals(1, index.scalars.size)
        assertEquals(0, index.objects.size)
        assertEquals(0, index.interfaces.size)
        assertEquals(0, index.unions.size)
        assertNull(index.queryTypeName)
        assertNull(index.mutationTypeName)
        assertNull(index.subscriptionTypeName)
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /**
     * Parses an inline SDL string by writing it to a temp file and passing it
     * through [SchemaParser.parse].
     */
    private fun parse(sdl: String): List<SchemaType> {
        val tempFile = createTempFile(sdl)
        return parser.parse(tempFile)
    }

    private fun createTempFile(content: String): File {
        val file = File.createTempFile("schema_test_", ".graphqls")
        file.writeText(content)
        file.deleteOnExit()
        return file
    }
}
