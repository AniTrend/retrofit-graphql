package co.anitrend.retrofit.graphql.converter.response

import co.anitrend.retrofit.graphql.model.GraphQLData
import co.anitrend.retrofit.graphql.model.GraphQLResponse
import co.anitrend.retrofit.graphql.model.request.GraphQLOperationRequest
import co.anitrend.retrofit.graphql.serialization.GraphQLResponseDecodingException
import co.anitrend.retrofit.graphql.serialization.GraphQLTransportCodec
import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class GraphQLResponseConverterTest {

    private class RecordingCodec : GraphQLTransportCodec {
        var decodedBody: String? = null
        var decodedResponseType: Type? = null
        var decodeException: RuntimeException? = null
        var decodeResult: Any = GraphQLResponse<Any>(data = GraphQLData.Present(null))

        override fun encodeRequest(
            request: GraphQLOperationRequest<*>,
            requestType: Type,
        ): String {
            throw AssertionError("encodeRequest should not be used by the response converter")
        }

        override fun <T : Any> decodeResponse(
            body: String,
            responseType: Type,
        ): T {
            decodedBody = body
            decodedResponseType = responseType
            decodeException?.let { throw it }
            @Suppress("UNCHECKED_CAST")
            return decodeResult as T
        }
    }

    private data class ViewerData(val login: String)

    private fun responseType(): Type =
        object : ParameterizedType {
            override fun getRawType(): Type = GraphQLResponse::class.java
            override fun getOwnerType(): Type? = null
            override fun getActualTypeArguments(): Array<Type> = arrayOf(ViewerData::class.java)
        }

    private fun responseBody(json: String): ResponseBody = ResponseBody.create(null, json)

    @Test
    fun `response converter forwards the raw body and complete parameterized type`() {
        val codec = RecordingCodec()
        val type = responseType()
        val converter = GraphQLResponseConverter<GraphQLResponse<ViewerData>>(codec = codec, type = type)
        val rawBody = """{"data":{"login":"octocat"}}"""

        val result = converter.convert(responseBody(rawBody))

        assertEquals(rawBody, codec.decodedBody)
        assertSame(type, codec.decodedResponseType)
        assertTrue(result.data is GraphQLData.Present)
    }

    @Test
    fun `codec decoding failures propagate and never become null`() {
        val codec =
            RecordingCodec().apply {
                decodeException =
                    GraphQLResponseDecodingException(
                        responseType = responseType(),
                        message = "backend boom",
                    )
            }
        val converter = GraphQLResponseConverter<GraphQLResponse<ViewerData>>(codec = codec, type = responseType())

        val error =
            assertThrows(GraphQLResponseDecodingException::class.java) {
                converter.convert(responseBody("""{"data":{}}"""))
            }

        assertSame(codec.decodeException, error)
        assertEquals("backend boom", error.message)
    }

    @Test
    fun `response body io failures propagate with type context and never become null`() {
        val codec = RecordingCodec()
        val type = responseType()
        val converter = GraphQLResponseConverter<GraphQLResponse<ViewerData>>(codec = codec, type = type)

        val error =
            assertThrows(IOException::class.java) {
                converter.convert(failingResponseBody())
            }

        assertTrue(error.message?.contains(type.toString()) == true)
    }

    @Test
    fun `successful conversions return the decoded value and never null`() {
        val codec =
            RecordingCodec().apply {
                decodeResult =
                    GraphQLResponse<ViewerData>(
                        data = GraphQLData.Present(ViewerData(login = "octocat")),
                    )
            }
        val converter = GraphQLResponseConverter<GraphQLResponse<ViewerData>>(codec = codec, type = responseType())

        val result = converter.convert(responseBody("""{"data":{"login":"octocat"}}"""))

        assertEquals("octocat", (result.data as GraphQLData.Present).value?.login)
    }

    private fun failingResponseBody(): ResponseBody =
        object : ResponseBody() {
            override fun contentType(): MediaType? = null

            override fun contentLength(): Long = 0L

            override fun source(): BufferedSource =
                object : ForwardingSource(Buffer()) {
                    override fun read(
                        sink: Buffer,
                        byteCount: Long,
                    ): Long = throw IOException("simulated I/O failure")
                }.buffer()
        }
}
