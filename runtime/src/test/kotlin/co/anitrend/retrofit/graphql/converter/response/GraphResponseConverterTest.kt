package co.anitrend.retrofit.graphql.converter.response

import co.anitrend.retrofit.graphql.model.GraphQLJson
import co.anitrend.retrofit.graphql.model.body.GraphContainer
import co.anitrend.retrofit.graphql.serialization.gson.GsonGraphQLJson
import com.google.gson.GsonBuilder
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class GraphResponseConverterTest {

    private val gsonJson: GraphQLJson = GsonGraphQLJson(
        GsonBuilder().serializeNulls().create(),
    )

    // ---- Test fixtures ----------------------------------------------------

    data class TestData(val id: String, val name: String?)

    private fun graphContainerType(typeArg: Type): Type =
        object : ParameterizedType {
            override fun getRawType(): Type = GraphContainer::class.java
            override fun getOwnerType(): Type? = null
            override fun getActualTypeArguments(): Array<Type> = arrayOf(typeArg)
        }

    private fun responseBody(json: String): ResponseBody =
        ResponseBody.create(null, json)

    // ---- Tests -------------------------------------------------------------

    @Test
    fun `response decode with Gson backend for simple type`() {
        val converter = GraphResponseConverter<TestData>(
            type = TestData::class.java,
            json = gsonJson,
        )

        val result = converter.convert(responseBody("""{"id":"123","name":"Test"}"""))
        assertNotNull(result)
        assertEquals("123", result?.id)
        assertEquals("Test", result?.name)
    }

    @Test
    fun `response decode with Gson backend for GraphContainer`() {
        val type = graphContainerType(TestData::class.java)
        val converter = GraphResponseConverter<GraphContainer<TestData>>(
            type = type,
            json = gsonJson,
        )

        val result = converter.convert(
            responseBody("""{"data":{"id":"123","name":"Test"}}"""),
        )
        assertNotNull(result)
        assertEquals("123", result?.data?.id)
        assertEquals("Test", result?.data?.name)
    }

    @Test
    fun `response decode with Gson backend for GraphContainer with errors`() {
        val type = graphContainerType(TestData::class.java)
        val converter = GraphResponseConverter<GraphContainer<TestData>>(
            type = type,
            json = gsonJson,
        )

        val result = converter.convert(
            responseBody("""{"data":null,"errors":[{"message":"Failed"}]}"""),
        )
        assertNotNull(result)
        assertNull(result?.data)
        assertNotNull(result?.errors)
        assertEquals(1, result?.errors?.size)
        assertEquals("Failed", result?.errors?.get(0)?.message)
    }

    @Test
    fun `response decode handles empty object response`() {
        val converter = GraphResponseConverter<TestData>(
            type = TestData::class.java,
            json = gsonJson,
        )

        val result = converter.convert(responseBody("{}"))
        // Gson deserializes {} into a TestData with default/null fields
        assertNotNull(result)
        assertNull(result?.id)
        assertNull(result?.name)
    }
}
