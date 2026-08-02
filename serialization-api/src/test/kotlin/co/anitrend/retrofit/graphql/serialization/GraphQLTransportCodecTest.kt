package co.anitrend.retrofit.graphql.serialization

import co.anitrend.retrofit.graphql.model.EmptyGraphQLVariables
import co.anitrend.retrofit.graphql.model.GraphQLData
import co.anitrend.retrofit.graphql.model.GraphQLResponse
import co.anitrend.retrofit.graphql.model.request.GraphQLOperationRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class GraphQLTransportCodecTest {

    private data class ViewerData(val login: String)

    /**
     * Fake codec that records the exact [Type] tokens and bodies forwarded
     * by the contract, proving the codec API compiles and forwards complete
     * generic types. It returns a constructed [GraphQLResponse] when the
     * target raw type is [GraphQLResponse], mirroring what a real backend
     * would produce.
     */
    private class RecordingCodec : GraphQLTransportCodec {
        var encodedRequestType: Type? = null
        var decodedResponseType: Type? = null
        var decodedBody: String? = null

        override fun encodeRequest(
            request: GraphQLOperationRequest<*>,
            requestType: Type,
        ): String {
            encodedRequestType = requestType
            return """{"query":"${request.query}","operationName":"${request.operationName}"}"""
        }

        override fun <T : Any> decodeResponse(
            body: String,
            responseType: Type,
        ): T {
            decodedBody = body
            decodedResponseType = responseType
            val raw = (responseType as? ParameterizedType)?.rawType
            @Suppress("UNCHECKED_CAST")
            return when (raw) {
                GraphQLResponse::class.java -> GraphQLResponse<Any>(data = GraphQLData.Present(null)) as T
                else -> body as T
            }
        }
    }

    private fun parameterizedType(raw: Class<*>, vararg typeArguments: Type): Type =
        object : ParameterizedType {
            override fun getRawType(): Type = raw
            override fun getOwnerType(): Type? = null
            override fun getActualTypeArguments(): Array<Type> = arrayOf(*typeArguments)
        }

    @Test
    fun `encodeRequest forwards the operation request and its complete type`() {
        val codec = RecordingCodec()
        val request =
            GraphQLOperationRequest<EmptyGraphQLVariables>(
                query = "query GetCurrentUser { viewer { login } }",
                operationName = "GetCurrentUser",
            )
        val requestType =
            parameterizedType(
                GraphQLOperationRequest::class.java,
                EmptyGraphQLVariables::class.java,
            )

        val body = codec.encodeRequest(request, requestType)

        assertSame(requestType, codec.encodedRequestType)
        assertTrue(body.contains("query GetCurrentUser { viewer { login } }"))
        assertTrue(body.contains("GetCurrentUser"))
    }

    @Test
    fun `decodeResponse forwards the raw body and complete response type and returns T`() {
        val codec = RecordingCodec()
        val rawBody = """{"data":{"login":"octocat"}}"""
        val responseType =
            parameterizedType(
                GraphQLResponse::class.java,
                ViewerData::class.java,
            )

        val decoded = codec.decodeResponse<GraphQLResponse<ViewerData>>(rawBody, responseType)

        assertEquals(rawBody, codec.decodedBody)
        assertSame(responseType, codec.decodedResponseType)
        assertTrue(responseType is ParameterizedType)
        (responseType as ParameterizedType).run {
            assertSame(GraphQLResponse::class.java, rawType)
            assertEquals(1, actualTypeArguments.size)
            assertSame(ViewerData::class.java, actualTypeArguments[0])
        }
        assertTrue(decoded.data is GraphQLData.Present)
    }

    @Test
    fun `decodeResponse handles a nullable data response type`() {
        val codec = RecordingCodec()
        val responseType =
            parameterizedType(
                GraphQLResponse::class.java,
                ViewerData::class.java,
            )

        val decoded: GraphQLResponse<ViewerData> =
            codec.decodeResponse("""{"data":null}""", responseType)

        assertTrue(decoded.data is GraphQLData.Present)
        assertEquals(null, (decoded.data as GraphQLData.Present).value)
    }

    @Test
    fun `encoding exception carries operation and request type context`() {
        val requestType =
            parameterizedType(
                GraphQLOperationRequest::class.java,
                EmptyGraphQLVariables::class.java,
            )

        val exception =
            GraphQLRequestEncodingException(
                operationName = "GetCurrentUser",
                requestType = requestType,
                message = "Failed to encode GraphQL request",
                cause = IllegalStateException("backend failure"),
            )

        assertEquals("GetCurrentUser", exception.operationName)
        assertSame(requestType, exception.requestType)
        assertEquals("Failed to encode GraphQL request", exception.message)
        assertTrue(exception.cause is IllegalStateException)
        assertTrue(exception is GraphQLTransportCodecException)
    }

    @Test
    fun `decoding exception carries response type and cause`() {
        val responseType =
            parameterizedType(
                GraphQLResponse::class.java,
                ViewerData::class.java,
            )
        val cause = IllegalArgumentException("malformed json")

        val exception =
            GraphQLResponseDecodingException(
                responseType = responseType,
                message = "Failed to decode GraphQL response",
                cause = cause,
            )

        assertSame(responseType, exception.responseType)
        assertEquals("Failed to decode GraphQL response", exception.message)
        assertSame(cause, exception.cause)
        assertTrue(exception is GraphQLTransportCodecException)
        assertTrue(exception is RuntimeException)
    }
}
