package co.anitrend.retrofit.graphql.serialization.kotlinx

import co.anitrend.retrofit.graphql.model.EmptyGraphQLVariables
import co.anitrend.retrofit.graphql.model.GraphQLJson
import co.anitrend.retrofit.graphql.model.GraphQLRequest
import co.anitrend.retrofit.graphql.model.GraphQLVariables
import co.anitrend.retrofit.graphql.model.attribute.GraphError
import co.anitrend.retrofit.graphql.model.body.GraphContainer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class KotlinxGraphQLJsonTest {

    private val json: GraphQLJson = KotlinxGraphQLJson(Json { ignoreUnknownKeys = true })

    // ---- Test fixtures ----------------------------------------------------

    @Serializable
    data class GetCurrentUserData(val viewer: Viewer?) {
        @Serializable
        data class Viewer(val login: String, val name: String? = null)
    }

    @Serializable
    data class GetMarketPlaceAppsVariables(
        val first: Int? = null,
        val after: String? = null,
    ) : GraphQLVariables

    @Serializable
    enum class Visibility {
        @SerialName("private")
        PRIVATE,

        PUBLIC,
    }

    enum class NonSerializableVisibility {
        PRIVATE,
    }

    // ---- Parameterized type helpers ---------------------------------------

    /**
     * Builds a [ParameterizedType] for [GraphContainer] parameterized with [typeArg].
     */
    private fun graphContainerType(typeArg: Type): Type =
        object : ParameterizedType {
            override fun getRawType(): Type = GraphContainer::class.java
            override fun getOwnerType(): Type? = null
            override fun getActualTypeArguments(): Array<Type> = arrayOf(typeArg)
        }

    /**
     * Builds a [ParameterizedType] for [GraphQLRequest] parameterized with [variablesType].
     */
    private fun graphQLRequestType(variablesType: Type): Type =
        object : ParameterizedType {
            override fun getRawType(): Type = GraphQLRequest::class.java
            override fun getOwnerType(): Type? = null
            override fun getActualTypeArguments(): Array<Type> = arrayOf(variablesType)
        }

    // ---- Tests -------------------------------------------------------------

    @Test
    fun `decode handles GraphContainer with parameterized type`() {
        val type = graphContainerType(GetCurrentUserData::class.java)
        val responseJson = """
            {
                "data": {
                    "viewer": {
                        "login": "octocat",
                        "name": "The Octocat"
                    }
                }
            }
        """.trimIndent()

        val result = json.decode<GraphContainer<GetCurrentUserData>>(responseJson, type)
        assertNotNull(result)
        assertEquals("octocat", result.data?.viewer?.login)
        assertEquals("The Octocat", result.data?.viewer?.name)
    }

    @Test
    fun `decode handles GraphContainer with errors (partial data)`() {
        val type = graphContainerType(GetCurrentUserData::class.java)
        val errorJson = """
            {
                "data": null,
                "errors": [
                    {
                        "message": "Something went wrong",
                        "locations": [
                            { "line": 2, "column": 3 }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val result = json.decode<GraphContainer<GetCurrentUserData>>(errorJson, type)
        assertNotNull(result)
        assertNull(result.data)
        assertNotNull(result.errors)
        assertEquals(1, result.errors?.size)
        assertEquals("Something went wrong", result.errors?.get(0)?.message)
        // locations are deserialized because GraphError is @Serializable
        assertEquals(2, result.errors?.get(0)?.locations?.get(0)?.line)
        assertEquals(3, result.errors?.get(0)?.locations?.get(0)?.column)
    }

    @Test
    fun `decode handles GraphContainer with both data and errors`() {
        val type = graphContainerType(GetCurrentUserData::class.java)
        val responseJson = """
            {
                "data": {
                    "viewer": {
                        "login": "octocat",
                        "name": "The Octocat"
                    }
                },
                "errors": [
                    {
                        "message": "Partial success warning",
                        "locations": [
                            { "line": 5, "column": 0 }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val result = json.decode<GraphContainer<GetCurrentUserData>>(responseJson, type)
        assertNotNull(result)
        // Both data and errors should be accessible
        assertNotNull(result.data)
        assertEquals("octocat", result.data?.viewer?.login)
        assertNotNull(result.errors)
        assertEquals(1, result.errors?.size)
        assertEquals("Partial success warning", result.errors?.get(0)?.message)
        assertEquals(5, result.errors?.get(0)?.locations?.get(0)?.line)
    }

    @Test
    fun `GraphError deserializes message and locations only`() {
        // path and extensions are @Transient for kotlinx, so they deserialize as null.
        // message and locations are included.
        val type = graphContainerType(GetCurrentUserData::class.java)
        val errorJson = """
            {
                "errors": [
                    {
                        "message": "Test error",
                        "path": ["user", "name"],
                        "extensions": { "code": "CUSTOM" },
                        "locations": [
                            { "line": 1, "column": 2 }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val result = json.decode<GraphContainer<GetCurrentUserData>>(errorJson, type)
        val error = result.errors?.get(0)
        assertNotNull(error)
        assertEquals("Test error", error?.message)
        assertEquals(1, error?.locations?.get(0)?.line)
        assertEquals(2, error?.locations?.get(0)?.column)
        // path and extensions are @Transient in kotlinx
        assertNull(error?.path)
        assertNull(error?.extensions)
    }

    @Test
    fun `encode handles GraphQLRequest with parameterized variables type`() {
        val type = graphQLRequestType(GetMarketPlaceAppsVariables::class.java)
        val request = GraphQLRequest(
            query = "query GetMarketPlaceApps(${'$'}first: Int) { marketplaceApps(first: ${'$'}first) { id } }",
            operationName = "GetMarketPlaceApps",
            variables = GetMarketPlaceAppsVariables(first = 10),
        )

        val encoded = json.encode(request, type)
        assertTrue(encoded.contains("\"query\""))
        assertTrue(encoded.contains("GetMarketPlaceApps"))
        assertTrue(encoded.contains("\"variables\""))
        assertTrue(encoded.contains("\"first\":10"))
    }

    @Test
    fun `encode preserves persisted query extensions for kotlinx`() {
        val type = graphQLRequestType(EmptyGraphQLVariables::class.java)
        val request =
            GraphQLRequest(
                query = "query GetCurrentUser { viewer { login } }",
                operationName = "GetCurrentUser",
                variables = EmptyGraphQLVariables,
            ).withPersistedQuery(
                sha256Hash = "abc123",
                version = 1,
            )

        val encoded = json.encode(request, type)

        assertTrue(encoded.contains("\"extensions\""))
        assertTrue(encoded.contains("\"persistedQuery\""))
        assertTrue(encoded.contains("\"sha256Hash\":\"abc123\""))
        assertTrue(encoded.contains("\"version\":1"))
    }

    @Test
    fun `encode preserves SerialName for enum values in recursively nested extensions`() {
        val type = graphQLRequestType(EmptyGraphQLVariables::class.java)
        val request = GraphQLRequest(
            query = "query GetCurrentUser { viewer { login } }",
            operationName = "GetCurrentUser",
            variables = EmptyGraphQLVariables,
            extensions = mapOf(
                "metadata" to mapOf(
                    "audit" to listOf(
                        mapOf(
                            "visibility" to Visibility.PRIVATE,
                        ),
                    ),
                ),
            ),
        )

        val encoded = json.encode(request, type)
        val visibility = Json.parseToJsonElement(encoded)
            .jsonObject
            .getValue("extensions")
            .jsonObject
            .getValue("metadata")
            .jsonObject
            .getValue("audit")
            .jsonArray
            .first()
            .jsonObject
            .getValue("visibility")
            .jsonPrimitive
            .content

        assertEquals("private", visibility)
        assertTrue(
            "Serializable enum extension value must not fall back to Enum.name",
            !encoded.contains("PRIVATE"),
        )
    }

    @Test
    fun `encode falls back to Enum name for non serializable enum extension values`() {
        val type = graphQLRequestType(EmptyGraphQLVariables::class.java)
        val request = GraphQLRequest(
            query = "query GetCurrentUser { viewer { login } }",
            operationName = "GetCurrentUser",
            variables = EmptyGraphQLVariables,
            extensions = mapOf(
                "metadata" to mapOf(
                    "visibility" to NonSerializableVisibility.PRIVATE,
                ),
            ),
        )

        val encoded = json.encode(request, type)
        val visibility = Json.parseToJsonElement(encoded)
            .jsonObject
            .getValue("extensions")
            .jsonObject
            .getValue("metadata")
            .jsonObject
            .getValue("visibility")
            .jsonPrimitive
            .content

        assertEquals("PRIVATE", visibility)
    }

    @Test
    fun `decode handles List of parameterized type`() {
        val viewerListType = object : ParameterizedType {
            override fun getRawType(): Type = List::class.java
            override fun getOwnerType(): Type? = null
            override fun getActualTypeArguments(): Array<Type> = arrayOf(GetCurrentUserData.Viewer::class.java)
        }

        val listJson = """
            [
                { "login": "octocat", "name": "Octocat" },
                { "login": "hubot", "name": "Hubot" }
            ]
        """.trimIndent()

        @Suppress("UNCHECKED_CAST")
        val result = json.decode<List<GetCurrentUserData.Viewer>>(listJson, viewerListType)
        assertEquals(2, result.size)
        assertEquals("octocat", result[0].login)
        assertEquals("hubot", result[1].login)
    }

    @Test
    fun `decode handles Map of parameterized type`() {
        val viewerMapType = object : ParameterizedType {
            override fun getRawType(): Type = Map::class.java
            override fun getOwnerType(): Type? = null
            override fun getActualTypeArguments(): Array<Type> = arrayOf(
                String::class.java,
                GetCurrentUserData.Viewer::class.java,
            )
        }

        val mapJson = """
            {
                "octocat": { "login": "octocat", "name": "Octocat" },
                "hubot": { "login": "hubot", "name": "Hubot" }
            }
        """.trimIndent()

        @Suppress("UNCHECKED_CAST")
        val result = json.decode<Map<String, GetCurrentUserData.Viewer>>(mapJson, viewerMapType)
        assertEquals(2, result.size)
        assertEquals("octocat", result["octocat"]?.login)
        assertEquals("Hubot", result["hubot"]?.name)
    }

    @Test
    fun `decode with raw Class type works as before`() {
        val decoded: GetCurrentUserData.Viewer = json.decode(
            """{"login": "octocat", "name": "Octocat"}""",
            GetCurrentUserData.Viewer::class.java,
        )
        assertEquals("octocat", decoded.login)
        assertEquals("Octocat", decoded.name)
    }

    @Test
    fun `decode produces actionable exception for unsupported type shape`() {
        val unsupportedType = object : Type {
            override fun getTypeName(): String = "UnsupportedType"
            override fun toString(): String = "UnsupportedType"
        }

        try {
            json.decode<Any>("{}", unsupportedType)
            fail("Expected exception was not thrown")
        } catch (e: Exception) {
            // Must NOT be a ClassCastException (the original bug)
            assertTrue(
                "Exception should not be ClassCastException, got: ${e.javaClass.simpleName}",
                e !is ClassCastException,
            )
            // Should be either IllegalArgumentException or SerializationException
            assertTrue(
                "Exception should be actionable (IllegalArgumentException or SerializationException), " +
                    "got: ${e.javaClass.simpleName}: ${e.message}",
                e is IllegalArgumentException || e is SerializationException,
            )
            assertTrue(
                "Exception message should mention the type. Got: ${e.message}",
                e.message?.contains("UnsupportedType") == true,
            )
        }
    }

    @Test
    fun `decode produces actionable exception when serializer not found`() {
        // A class that is NOT @Serializable
        class NonSerializable(val x: Int)

        try {
            json.decode<NonSerializable>("""{"x": 1}""", NonSerializable::class.java)
            fail("Expected exception was not thrown")
        } catch (e: Exception) {
            assertTrue(
                "Exception should not be ClassCastException, got: ${e.javaClass.simpleName}",
                e !is ClassCastException,
            )
            assertTrue(
                "Exception should be SerializationException or IllegalArgumentException. " +
                    "Got: ${e.javaClass.simpleName}: ${e.message}",
                e is SerializationException || e is IllegalArgumentException,
            )
        }
    }

    @Test
    fun `encode with null type falls back to runtime class`() {
        val viewer = GetCurrentUserData.Viewer("octocat", "Octocat")
        val encoded = json.encode(viewer, null)
        assertTrue(encoded.contains("octocat"))
        assertTrue(encoded.contains("Octocat"))
    }

    @Test
    fun `round-trip encode then decode preserves data`() {
        val data = GetCurrentUserData(
            viewer = GetCurrentUserData.Viewer("octocat", "The Octocat"),
        )
        val encoded = json.encode(data)
        val decoded = json.decode<GetCurrentUserData>(encoded, GetCurrentUserData::class.java)

        assertEquals(data.viewer?.login, decoded.viewer?.login)
        assertEquals(data.viewer?.name, decoded.viewer?.name)
    }
}
