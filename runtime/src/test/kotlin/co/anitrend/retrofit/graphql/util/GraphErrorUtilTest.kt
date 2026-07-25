package co.anitrend.retrofit.graphql.util

import co.anitrend.retrofit.graphql.model.GraphQLJson
import co.anitrend.retrofit.graphql.model.attribute.GraphError
import co.anitrend.retrofit.graphql.serialization.gson.GsonGraphQLJson
import com.google.gson.GsonBuilder
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Response

class GraphErrorUtilTest {

    private val gsonJson: GraphQLJson = GsonGraphQLJson(
        GsonBuilder().serializeNulls().create(),
    )

    // ---- Helpers -----------------------------------------------------------

    private fun errorResponse(jsonBody: String): Response<Any> =
        Response.error(400, ResponseBody.create(null, jsonBody))

    // ---- Tests -------------------------------------------------------------

    @Test
    fun `getError returns errors from response with default Gson path`() {
        val response: Response<Any> = errorResponse(
            """{"errors":[{"message":"Test error"}]}""",
        )

        val errors = response.getError()
        assertNotNull(errors)
        assertEquals(1, errors?.size)
        assertEquals("Test error", errors?.get(0)?.message)
    }

    @Test
    fun `getError returns null when response has no error body`() {
        val response: Response<Any> = Response.success(null)
        val errors = response.getError()
        assertNull(errors)
    }

    @Test
    fun `getError with Gson backend returns errors with all fields`() {
        val response: Response<Any> = errorResponse(
            """{"errors":[{"message":"Test","path":["user"],"extensions":{"code":"X"}}]}""",
        )

        val errors = response.getError(gsonJson)
        assertNotNull(errors)
        assertEquals("Test", errors?.get(0)?.message)
        assertEquals(listOf("user"), errors?.get(0)?.path)
        assertEquals(mapOf("code" to "X"), errors?.get(0)?.extensions)
    }

    @Test
    fun `getError returns null when response is null`() {
        val errors: List<GraphError>? = null.getError()
        assertNull(errors)
    }

    @Test
    fun `getError with GraphQLJson overload works with Gson backend`() {
        val response: Response<Any> = errorResponse(
            """{"errors":[{"message":"Gson error"}]}""",
        )

        val errors = response.getError(gsonJson)
        assertNotNull(errors)
        assertEquals(1, errors?.size)
        assertEquals("Gson error", errors?.get(0)?.message)
    }
}
