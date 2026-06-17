package co.anitrend.retrofit.graphql.codegen

import co.anitrend.retrofit.graphql.codegen.generate.OperationConstantsGenerator
import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import co.anitrend.retrofit.graphql.codegen.model.OperationType
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import graphql.language.OperationDefinition
import graphql.parser.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OperationConstantsGeneratorTest {

    private val parser = Parser()

    // ---------------------------------------------------------------------------
    // Named operations
    // ---------------------------------------------------------------------------

    @Test
    fun `should generate GraphQLOperations with Query, Mutation, Subscription for named operations`() {
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetUser",
                type = OperationType.QUERY,
                document = "query GetUser { user { id name } }",
                sourceFile = "user.graphql",
            ),
            GraphQLOperationInfo(
                name = "UpdateUser",
                type = OperationType.MUTATION,
                document = "mutation UpdateUser(${'$'}id: ID!, ${'$'}name: String!) { updateUser(id: ${'$'}id, name: ${'$'}name) { id name } }",
                sourceFile = "user.graphql",
            ),
            GraphQLOperationInfo(
                name = "OnUserUpdated",
                type = OperationType.SUBSCRIPTION,
                document = "subscription OnUserUpdated { userUpdated { id name } }",
                sourceFile = "user.graphql",
            ),
        )

        val fileSpec: FileSpec =
            OperationConstantsGenerator.generate(operations, "com.example")

        // Top-level object
        val graphQLOps: TypeSpec = fileSpec.members
            .filterIsInstance<TypeSpec>()
            .find { it.name == "GraphQLOperations" }!!

        // Inner object types
        val queryType = graphQLOps.typeSpecs.find { it.name == "Query" }
        val mutationType = graphQLOps.typeSpecs.find { it.name == "Mutation" }
        val subscriptionType = graphQLOps.typeSpecs.find { it.name == "Subscription" }

        assertNotNull("Query object should exist", queryType)
        assertNotNull("Mutation object should exist", mutationType)
        assertNotNull("Subscription object should exist", subscriptionType)

        // Query should contain GetUser
        assertEquals(1, queryType!!.propertySpecs.size)
        val getUserProp = queryType.propertySpecs[0]
        assertEquals("GetUser", getUserProp.name)
        assertEquals("kotlin.String", getUserProp.type.toString())
        assertTrue(
            "Initializer should contain \"GetUser\"",
            getUserProp.initializer.toString().contains("\"GetUser\"")
        )

        // Mutation should contain UpdateUser
        assertEquals(1, mutationType!!.propertySpecs.size)
        val updateUserProp = mutationType.propertySpecs[0]
        assertEquals("UpdateUser", updateUserProp.name)
        assertTrue(
            "Initializer should contain \"UpdateUser\"",
            updateUserProp.initializer.toString().contains("\"UpdateUser\"")
        )

        // Subscription should contain OnUserUpdated
        assertEquals(1, subscriptionType!!.propertySpecs.size)
        val onUserUpdatedProp = subscriptionType.propertySpecs[0]
        assertEquals("OnUserUpdated", onUserUpdatedProp.name)
        assertTrue(
            "Initializer should contain \"OnUserUpdated\"",
            onUserUpdatedProp.initializer.toString().contains("\"OnUserUpdated\"")
        )
    }

    @Test
    fun `should produce empty inner objects when no operations of a given type`() {
        val operations = listOf(
            GraphQLOperationInfo(
                name = "GetUser",
                type = OperationType.QUERY,
                document = "query GetUser { user { id } }",
                sourceFile = "user.graphql",
            ),
        )

        val fileSpec = OperationConstantsGenerator.generate(operations, "com.example")

        val graphQLOps = fileSpec.members
            .filterIsInstance<TypeSpec>()
            .find { it.name == "GraphQLOperations" }!!

        val queryType = graphQLOps.typeSpecs.find { it.name == "Query" }
        val mutationType = graphQLOps.typeSpecs.find { it.name == "Mutation" }
        val subscriptionType = graphQLOps.typeSpecs.find { it.name == "Subscription" }

        assertNotNull(queryType)
        assertNotNull(mutationType)
        assertNotNull(subscriptionType)

        // Query has 1 property, Mutation and Subscription have 0
        assertEquals(1, queryType!!.propertySpecs.size)
        assertEquals(0, mutationType!!.propertySpecs.size)
        assertEquals(0, subscriptionType!!.propertySpecs.size)
    }

    // ---------------------------------------------------------------------------
    // Duplicate operations
    // ---------------------------------------------------------------------------

    @Test
    fun `should detect duplicate operation names from GraphQL documents`() {
        val documentText = """
            query GetUser {
                user(id: "1") { name }
            }
            
            query GetUser {
                user(id: "2") { email }
            }
        """.trimIndent()

        val document = parser.parseDocument(documentText)
        val operations = document.definitions
            .filterIsInstance<OperationDefinition>()

        // graphql-java parses both; duplicate names are not rejected at the parser level
        assertEquals("Should parse both operations", 2, operations.size)

        val names = operations.map { it.name }
        assertEquals("Both should be named GetUser", listOf("GetUser", "GetUser"), names)

        // Validate: duplicate names should be detected before code generation
        val duplicates = names.groupingBy { it }.eachCount().filter { it.value > 1 }
        assertTrue("Duplicate operation names should be detected", duplicates.isNotEmpty())
        assertEquals("GetUser should appear twice", 1, duplicates.size)
        assertEquals("GetUser count should be 2", 2, duplicates["GetUser"])

        // Verify that passing duplicates to the generator produces duplicate const vals
        val ops = operations.map { def ->
            GraphQLOperationInfo(
                name = def.name!!,
                type = OperationType.fromGraphQLJava(def.operation.name),
                document = graphql.language.AstPrinter.printAst(def),
                sourceFile = "test.graphql",
            )
        }

        val fileSpec = OperationConstantsGenerator.generate(ops, "com.example")
        val generatedSource = fileSpec.toString()

        // Both const vals are emitted (duplicate names in Kotlin would fail compilation)
        val occurrences = generatedSource.lines().count { "GetUser" in it }
        assertTrue(
            "GetUser should appear multiple times (duplicate const vals)",
            occurrences >= 2
        )
    }
}
