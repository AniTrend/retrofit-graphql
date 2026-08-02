package co.anitrend.retrofit.graphql.serialization.gson

import co.anitrend.retrofit.graphql.model.GraphQLJson
import co.anitrend.retrofit.graphql.model.body.GraphContainer
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class GsonGraphQLJsonTest {

    private val json: GraphQLJson = GsonGraphQLJson(
        GsonBuilder().serializeNulls().create(),
    )

    // ---- Test fixtures ----------------------------------------------------

    data class GetCurrentUserData(val viewer: Viewer?) {
        data class Viewer(val login: String, val name: String? = null)
    }

    private fun graphContainerType(typeArg: Type): Type =
        object : ParameterizedType {
            override fun getRawType(): Type = GraphContainer::class.java
            override fun getOwnerType(): Type? = null
            override fun getActualTypeArguments(): Array<Type> = arrayOf(typeArg)
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
    fun `decode handles GraphContainer with errors`() {
        val type = graphContainerType(GetCurrentUserData::class.java)
        val errorJson = """
            {
                "data": null,
                "errors": [
                    {
                        "message": "Something went wrong"
                    }
                ]
            }
        """.trimIndent()

        val result = json.decode<GraphContainer<GetCurrentUserData>>(errorJson, type)
        assertNull(result.data)
        assertNotNull(result.errors)
        assertEquals(1, result.errors?.size)
        assertEquals("Something went wrong", result.errors?.get(0)?.message)
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
        assertNotNull(result.data)
        assertEquals("octocat", result.data?.viewer?.login)
        assertNotNull(result.errors)
        assertEquals(1, result.errors?.size)
        assertEquals("Partial success warning", result.errors?.get(0)?.message)
    }

    @Test
    fun `decode handles partial data with null data and errors`() {
        val type = graphContainerType(GetCurrentUserData::class.java)
        val partialJson = """
            {
                "data": null,
                "errors": [
                    {
                        "message": "Query failed",
                        "path": ["user", "name"],
                        "extensions": { "code": "CUSTOM" }
                    }
                ]
            }
        """.trimIndent()

        val result = json.decode<GraphContainer<GetCurrentUserData>>(partialJson, type)
        assertNull(result.data)
        assertNotNull(result.errors)
        assertEquals(1, result.errors?.size)
        assertEquals("Query failed", result.errors?.get(0)?.message)
        // Gson handles all fields including path and extensions
        assertEquals(listOf("user", "name"), result.errors?.get(0)?.path)
        assertEquals(mapOf("code" to "CUSTOM"), result.errors?.get(0)?.extensions)
    }

    @Test
    fun `encode with Gson serializes correctly`() {
        val data = GetCurrentUserData(
            viewer = GetCurrentUserData.Viewer("octocat", "Octocat"),
        )
        val encoded = json.encode(data)
        assertNotNull(encoded)
        assertEquals(
            """{"viewer":{"login":"octocat","name":"Octocat"}}""",
            encoded,
        )
    }

    @Test
    fun `encode with explicit type uses type token`() {
        val data = GetCurrentUserData(null)
        val encoded = json.encode(data, GetCurrentUserData::class.java)
        assertNotNull(encoded)
        assertEquals("""{"viewer":null}""", encoded)
    }

    @Test
    fun `round-trip encode then decode preserves data`() {
        val original = GetCurrentUserData(
            viewer = GetCurrentUserData.Viewer("octocat", "The Octocat"),
        )
        val encoded = json.encode(original)
        val decoded = json.decode<GetCurrentUserData>(encoded, GetCurrentUserData::class.java)

        assertEquals(original.viewer?.login, decoded.viewer?.login)
        assertEquals(original.viewer?.name, decoded.viewer?.name)
    }
}
