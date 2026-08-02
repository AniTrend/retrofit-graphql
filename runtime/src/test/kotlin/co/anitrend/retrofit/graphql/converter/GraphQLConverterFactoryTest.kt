package co.anitrend.retrofit.graphql.converter

import co.anitrend.retrofit.graphql.model.EmptyGraphQLVariables
import co.anitrend.retrofit.graphql.model.GraphQLData
import co.anitrend.retrofit.graphql.model.GraphQLDocumentRegistry
import co.anitrend.retrofit.graphql.model.GraphQLResponse
import co.anitrend.retrofit.graphql.model.request.GraphQLOperationRequest
import co.anitrend.retrofit.graphql.serialization.GraphQLTransportCodec
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class GraphQLConverterFactoryTest {

    private class RecordingCodec : GraphQLTransportCodec {
        var encodeCalls: Int = 0
            private set
        var decodeCalls: Int = 0
            private set

        override fun encodeRequest(
            request: GraphQLOperationRequest<*>,
            requestType: Type,
        ): String {
            encodeCalls += 1
            return """{"query":"${request.query}","operationName":"${request.operationName}"}"""
        }

        override fun <T : Any> decodeResponse(
            body: String,
            responseType: Type,
        ): T {
            decodeCalls += 1
            @Suppress("UNCHECKED_CAST")
            return GraphQLResponse<Any>(data = GraphQLData.Present(null)) as T
        }
    }

    private class ThrowingRegistry : GraphQLDocumentRegistry {
        override fun document(operationName: String): String? {
            throw AssertionError("Registry must not be consulted for a document-carrying neutral request")
        }

        override fun hash(operationName: String): String? {
            throw AssertionError("Registry must not be consulted for a document-carrying neutral request")
        }
    }

    private fun retrofit(): Retrofit = Retrofit.Builder().baseUrl("https://example.com/").build()

    private fun requestType(): Type =
        object : ParameterizedType {
            override fun getRawType(): Type = GraphQLOperationRequest::class.java
            override fun getOwnerType(): Type? = null
            override fun getActualTypeArguments(): Array<Type> = arrayOf(EmptyGraphQLVariables::class.java)
        }

    private fun responseType(): Type =
        object : ParameterizedType {
            override fun getRawType(): Type = GraphQLResponse::class.java
            override fun getOwnerType(): Type? = null
            override fun getActualTypeArguments(): Array<Type> = arrayOf(EmptyGraphQLVariables::class.java)
        }

    private fun sampleRequest(): GraphQLOperationRequest<EmptyGraphQLVariables> =
        GraphQLOperationRequest(
            query = "query GetCurrentUser { viewer { login } }",
            operationName = "GetCurrentUser",
        )

    @Test
    fun `create accepts named codec and registry arguments without a context`() {
        val codec = RecordingCodec()

        val factory = GraphQLConverterFactory.create(registry = ThrowingRegistry(), codec = codec)
        val converter =
            factory.requestBodyConverter(
                requestType(),
                emptyArray(),
                emptyArray(),
                retrofit(),
            ) as retrofit2.Converter<Any, RequestBody>

        converter.convert(sampleRequest())

        assertEquals(1, codec.encodeCalls)
    }

    @Test
    fun `create accepts a codec without a registry`() {
        val factory = GraphQLConverterFactory.create(codec = RecordingCodec())

        assertNotNull(
            factory.requestBodyConverter(requestType(), emptyArray(), emptyArray(), retrofit()),
        )
        assertNotNull(
            factory.responseBodyConverter(responseType(), emptyArray(), retrofit()),
        )
    }

    @Test
    fun `request conversion never consults the registry while the request carries its document`() {
        val factory = GraphQLConverterFactory.create(registry = ThrowingRegistry(), codec = RecordingCodec())
        val converter =
            factory.requestBodyConverter(
                requestType(),
                emptyArray(),
                emptyArray(),
                retrofit(),
            ) as retrofit2.Converter<Any, RequestBody>

        val requestBody = converter.convert(sampleRequest())
        val buffer = Buffer()
        requestBody?.writeTo(buffer)

        assertTrue(buffer.readUtf8().contains("query GetCurrentUser { viewer { login } }"))
    }

    @Test
    fun `request body converter uses the provided codec and json media type`() {
        val codec = RecordingCodec()
        val factory = GraphQLConverterFactory.create(codec = codec)
        val converter =
            factory.requestBodyConverter(
                requestType(),
                emptyArray(),
                emptyArray(),
                retrofit(),
            ) as retrofit2.Converter<Any, RequestBody>

        val requestBody = converter.convert(sampleRequest())

        assertEquals(1, codec.encodeCalls)
        assertEquals("application", requestBody?.contentType()?.type)
        assertEquals("json", requestBody?.contentType()?.subtype)
    }

    @Test
    fun `response body converter decodes through the provided codec`() {
        val codec = RecordingCodec()
        val factory = GraphQLConverterFactory.create(codec = codec)
        val converter =
            factory.responseBodyConverter(
                responseType(),
                emptyArray(),
                retrofit(),
            ) as retrofit2.Converter<ResponseBody, Any>

        val result = converter.convert(ResponseBody.create(null, """{"data":null}"""))

        assertEquals(1, codec.decodeCalls)
        assertTrue(result is GraphQLResponse<*>)
    }

    @Test
    fun `ResponseBody response types are left to the built-in passthrough`() {
        val factory = GraphQLConverterFactory.create(codec = RecordingCodec())

        assertNull(
            factory.responseBodyConverter(ResponseBody::class.java, emptyArray(), retrofit()),
        )
    }
}
