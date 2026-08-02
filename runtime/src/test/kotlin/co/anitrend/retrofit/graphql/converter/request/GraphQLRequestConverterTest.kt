package co.anitrend.retrofit.graphql.converter.request

import co.anitrend.retrofit.graphql.model.EmptyGraphQLVariables
import co.anitrend.retrofit.graphql.model.request.GraphQLOperationRequest
import co.anitrend.retrofit.graphql.serialization.GraphQLRequestEncodingException
import co.anitrend.retrofit.graphql.serialization.GraphQLTransportCodec
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class GraphQLRequestConverterTest {

    private class RecordingCodec : GraphQLTransportCodec {
        var encodedRequest: GraphQLOperationRequest<*>? = null
        var encodedRequestType: Type? = null
        var encodeException: RuntimeException? = null

        override fun encodeRequest(
            request: GraphQLOperationRequest<*>,
            requestType: Type,
        ): String {
            encodedRequest = request
            encodedRequestType = requestType
            encodeException?.let { throw it }
            return """{"query":"${request.query}","operationName":"${request.operationName}"}"""
        }

        override fun <T : Any> decodeResponse(
            body: String,
            responseType: Type,
        ): T {
            throw AssertionError("decodeResponse should not be used by the request converter")
        }
    }

    private fun requestType(): Type =
        object : ParameterizedType {
            override fun getRawType(): Type = GraphQLOperationRequest::class.java
            override fun getOwnerType(): Type? = null
            override fun getActualTypeArguments(): Array<Type> = arrayOf(EmptyGraphQLVariables::class.java)
        }

    private fun sampleRequest(): GraphQLOperationRequest<EmptyGraphQLVariables> =
        GraphQLOperationRequest(
            query = "query GetCurrentUser { viewer { login } }",
            operationName = "GetCurrentUser",
        )

    @Test
    fun `request converter forwards the complete parameterized type to the codec`() {
        val codec = RecordingCodec()
        val type = requestType()
        val converter = GraphQLRequestConverter(codec = codec, type = type)
        val request = sampleRequest()

        val requestBody = converter.convert(request)

        assertSame(request, codec.encodedRequest)
        assertSame(type, codec.encodedRequestType)
        val buffer = Buffer()
        requestBody.writeTo(buffer)
        assertTrue(buffer.readUtf8().contains("GetCurrentUser"))
    }

    @Test
    fun `unsupported request body fails with the actual and expected types`() {
        val converter = GraphQLRequestConverter(codec = RecordingCodec(), type = requestType())

        val error =
            assertThrows(IllegalArgumentException::class.java) {
                converter.convert("not a graphql request")
            }

        assertTrue(error.message?.contains("java.lang.String") == true)
        assertTrue(error.message?.contains("GraphQLOperationRequest") == true)
    }

    @Test
    fun `codec encoding failures propagate unchanged`() {
        val codec =
            RecordingCodec().apply {
                encodeException =
                    GraphQLRequestEncodingException(
                        operationName = "GetCurrentUser",
                        requestType = requestType(),
                        message = "backend boom",
                    )
            }
        val converter = GraphQLRequestConverter(codec = codec, type = requestType())

        val error =
            assertThrows(GraphQLRequestEncodingException::class.java) {
                converter.convert(sampleRequest())
            }

        assertSame(codec.encodeException, error)
        assertEquals("GetCurrentUser", error.operationName)
    }
}
