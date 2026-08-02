package co.anitrend.retrofit.graphql.serialization.kotlinx

import co.anitrend.retrofit.graphql.model.EmptyGraphQLVariables
import co.anitrend.retrofit.graphql.model.GraphQLData
import co.anitrend.retrofit.graphql.model.GraphQLPathSegment
import co.anitrend.retrofit.graphql.model.GraphQLResponse
import co.anitrend.retrofit.graphql.model.GraphQLResponseError
import co.anitrend.retrofit.graphql.model.GraphQLValue
import co.anitrend.retrofit.graphql.model.GraphQLVariables
import co.anitrend.retrofit.graphql.model.request.GraphQLOperationRequest
import co.anitrend.retrofit.graphql.serialization.GraphQLRequestEncodingException
import co.anitrend.retrofit.graphql.serialization.GraphQLResponseDecodingException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.math.BigDecimal

class KotlinxGraphQLTransportCodecTest {

    private val codec = KotlinxGraphQLTransportCodec()
    private val json = Json { ignoreUnknownKeys = true }

    // ---- Test fixtures ----------------------------------------------------

    @Serializable
    data class GetUserVariables(val login: String, val limit: Int) : GraphQLVariables

    @Serializable
    data class GetCurrentUserData(val viewer: Viewer? = null) {
        @Serializable
        data class Viewer(val login: String, val name: String? = null)
    }

    @Serializable
    data class PriceData(val price: String, val items: List<Item>) {
        @Serializable
        data class Item(val name: String, val quantity: Int)
    }

    private fun parameterizedType(
        raw: Class<*>,
        vararg typeArguments: Type,
    ): Type =
        object : ParameterizedType {
            override fun getRawType(): Type = raw
            override fun getOwnerType(): Type? = null
            override fun getActualTypeArguments(): Array<Type> = arrayOf(*typeArguments)
        }

    private fun requestType(variablesType: Type): Type =
        parameterizedType(GraphQLOperationRequest::class.java, variablesType)

    private fun responseType(typeArgument: Type): Type =
        parameterizedType(GraphQLResponse::class.java, typeArgument)

    // ---- Request encoding --------------------------------------------------

    @Test
    fun `encodeRequest writes the neutral envelope with typed variables and extensions`() {
        val request =
            GraphQLOperationRequest<GetUserVariables>(
                query = "query GetUser(\$login: String!) { user(login: \$login) { name } }",
                operationName = "GetUser",
                variables = GetUserVariables(login = "octocat", limit = 10),
                extensions =
                    GraphQLValue.ObjectValue(
                        fields =
                            mapOf(
                                "persistedQuery" to
                                    GraphQLValue.ObjectValue(
                                        fields =
                                            mapOf(
                                                "version" to GraphQLValue.NumberValue(BigDecimal(1)),
                                                "sha256Hash" to GraphQLValue.StringValue("abc123"),
                                            ),
                                    ),
                            ),
                    ),
            )

        val envelope =
            json.parseToJsonElement(
                codec.encodeRequest(request, requestType(GetUserVariables::class.java)),
            ).jsonObject

        assertEquals(
            "query GetUser(\$login: String!) { user(login: \$login) { name } }",
            envelope["query"]?.jsonPrimitive?.content,
        )
        assertEquals("GetUser", envelope["operationName"]?.jsonPrimitive?.content)
        val variables = envelope["variables"]?.jsonObject
        assertEquals("octocat", variables?.get("login")?.jsonPrimitive?.content)
        assertEquals("10", variables?.get("limit")?.jsonPrimitive?.content)
        val persistedQuery = envelope["extensions"]?.jsonObject?.get("persistedQuery")?.jsonObject
        assertEquals("1", persistedQuery?.get("version")?.jsonPrimitive?.content)
        assertEquals("abc123", persistedQuery?.get("sha256Hash")?.jsonPrimitive?.content)
    }

    @Test
    fun `encodeRequest writes explicit nulls for absent variables and extensions`() {
        val request =
            GraphQLOperationRequest<EmptyGraphQLVariables>(
                query = "query GetCurrentUser { viewer { login } }",
                operationName = "GetCurrentUser",
            )

        val envelope =
            json.parseToJsonElement(
                codec.encodeRequest(request, requestType(EmptyGraphQLVariables::class.java)),
            ).jsonObject

        assertTrue(envelope.containsKey("variables"))
        assertTrue(envelope["variables"]!!.jsonPrimitive is JsonNull)
        assertTrue(envelope.containsKey("extensions"))
        assertTrue(envelope["extensions"]!!.jsonPrimitive is JsonNull)
    }

    @Test
    fun `encodeRequest wraps backend failures with request context`() {
        // A variables class without @Serializable cannot be resolved by kotlinx.serialization.
        class NotSerializableVariables : GraphQLVariables
        val request =
            GraphQLOperationRequest<NotSerializableVariables>(
                query = "query Broken { viewer { login } }",
                operationName = "Broken",
                variables = NotSerializableVariables(),
            )
        val type = requestType(NotSerializableVariables::class.java)

        val error =
            assertThrows(GraphQLRequestEncodingException::class.java) {
                codec.encodeRequest(request, type)
            }

        assertEquals("Broken", error.operationName)
        assertSame(type, error.requestType)
        assertTrue(error.cause is SerializationException)
    }

    // ---- Response decoding: data presence -----------------------------------

    @Test
    fun `decodeResponse distinguishes missing data from explicit data null`() {
        val type = responseType(GetCurrentUserData::class.java)

        val missing =
            codec.decodeResponse<GraphQLResponse<GetCurrentUserData>>(
                """{"errors":[{"message":"boom"}]}""",
                type,
            )
        assertTrue(missing.data is GraphQLData.Absent)
        assertEquals(listOf("boom"), missing.errors.map { it.message })

        val explicit =
            codec.decodeResponse<GraphQLResponse<GetCurrentUserData>>(
                """{"data":null}""",
                type,
            )
        assertTrue(explicit.data is GraphQLData.Present)
        assertNull((explicit.data as GraphQLData.Present).value)
    }

    @Test
    fun `decodeResponse treats missing errors and extensions as empty or absent`() {
        val type = responseType(GetCurrentUserData::class.java)

        val result =
            codec.decodeResponse<GraphQLResponse<GetCurrentUserData>>(
                """{"data":{"viewer":{"login":"octocat"}}}""",
                type,
            )

        assertEquals("octocat", (result.data as GraphQLData.Present).value?.viewer?.login)
        assertTrue(result.errors.isEmpty())
        assertNull(result.extensions)
    }

    @Test
    fun `decodeResponse treats explicit null errors and extensions as absent`() {
        val type = responseType(GetCurrentUserData::class.java)

        val result =
            codec.decodeResponse<GraphQLResponse<GetCurrentUserData>>(
                """{"data":null,"errors":null,"extensions":null}""",
                type,
            )

        assertTrue(result.data is GraphQLData.Present)
        assertTrue(result.errors.isEmpty())
        assertNull(result.extensions)
    }

    // ---- Response decoding: data values --------------------------------------

    @Test
    fun `decodeResponse decodes nested data values`() {
        val type = responseType(PriceData::class.java)

        val result =
            codec.decodeResponse<GraphQLResponse<PriceData>>(
                """{"data":{"price":"0.30000000000000004","items":[{"name":"a","quantity":2}]}}""",
                type,
            )

        val data = (result.data as GraphQLData.Present).value
        assertEquals("0.30000000000000004", data?.price)
        assertEquals("a", data?.items?.get(0)?.name)
        assertEquals(2, data?.items?.get(0)?.quantity)
    }

    @Test
    fun `decodeResponse preserves nested parameterized data types`() {
        val type = responseType(parameterizedType(List::class.java, PriceData::class.java))

        val result =
            codec.decodeResponse<GraphQLResponse<List<PriceData>>>(
                """{"data":[{"price":"1.50","items":[{"name":"a","quantity":2}]}]}""",
                type,
            )

        val data = (result.data as GraphQLData.Present).value
        assertEquals(1, data?.size)
        assertEquals("a", data?.first()?.items?.first()?.name)
        assertEquals(2, data?.first()?.items?.first()?.quantity)
    }

    @Test
    fun `decodeResponse keeps data and errors together`() {
        val type = responseType(GetCurrentUserData::class.java)

        val result =
            codec.decodeResponse<GraphQLResponse<GetCurrentUserData>>(
                """{"data":{"viewer":{"login":"octocat"}},"errors":[{"message":"partial"}]}""",
                type,
            )

        assertEquals("octocat", (result.data as GraphQLData.Present).value?.viewer?.login)
        assertEquals(listOf("partial"), result.errors.map { it.message })
    }

    // ---- Response decoding: extensions and BigDecimal precision ----------------

    @Test
    fun `decodeResponse preserves BigDecimal precision in top-level and error extensions`() {
        val type = responseType(GetCurrentUserData::class.java)

        val result =
            codec.decodeResponse<GraphQLResponse<GetCurrentUserData>>(
                """{"data":{"viewer":{"login":"octocat"}},""" +
                    """"extensions":{"cost":{"requested":1.50,"max":9.999999999999999999}},""" +
                    """"errors":[{"message":"warn","extensions":{"code":"CUSTOM","retry":false,"note":null}}]}""",
                type,
            )

        val cost = (result.extensions?.fields?.get("cost") as GraphQLValue.ObjectValue).fields
        assertEquals(
            0,
            (cost["requested"] as GraphQLValue.NumberValue).value.compareTo(BigDecimal("1.50")),
        )
        assertEquals(
            0,
            (cost["max"] as GraphQLValue.NumberValue).value.compareTo(BigDecimal("9.999999999999999999")),
        )
        val errorExtensions = requireNotNull(result.errors[0].extensions).fields
        assertEquals("CUSTOM", (errorExtensions["code"] as GraphQLValue.StringValue).value)
        assertEquals(false, (errorExtensions["retry"] as GraphQLValue.BooleanValue).value)
        assertTrue(errorExtensions["note"] is GraphQLValue.Null)
    }

    // ---- Response decoding: error shape validation ------------------------------

    @Test
    fun `decodeResponse parses error locations`() {
        val type = responseType(GetCurrentUserData::class.java)

        val result =
            codec.decodeResponse<GraphQLResponse<GetCurrentUserData>>(
                """{"data":null,"errors":[{"message":"syntax","locations":[{"line":1,"column":10}]}]}""",
                type,
            )

        val locations = result.errors[0].locations
        assertEquals(GraphQLResponseError.Location(line = 1, column = 10), locations?.get(0))
    }

    @Test
    fun `decodeResponse parses mixed field and index path segments`() {
        val type = responseType(GetCurrentUserData::class.java)

        val result =
            codec.decodeResponse<GraphQLResponse<GetCurrentUserData>>(
                """{"data":null,"errors":[{"message":"bad index","path":["viewer","repositories",3]}]}""",
                type,
            )

        val path = result.errors[0].path
        assertEquals(
            listOf(
                GraphQLPathSegment.Field("viewer"),
                GraphQLPathSegment.Field("repositories"),
                GraphQLPathSegment.Index(3),
            ),
            path,
        )
    }

    @Test
    fun `decodeResponse fails when an error message is missing`() {
        val type = responseType(GetCurrentUserData::class.java)

        val error =
            assertThrows(GraphQLResponseDecodingException::class.java) {
                codec.decodeResponse<GraphQLResponse<GetCurrentUserData>>(
                    """{"data":null,"errors":[{"path":["viewer"]}]}""",
                    type,
                )
            }

        assertSame(type, error.responseType)
        assertTrue(error.message?.contains("message") == true)
    }

    @Test
    fun `decodeResponse fails when an error message is not a string`() {
        val type = responseType(GetCurrentUserData::class.java)

        assertThrows(GraphQLResponseDecodingException::class.java) {
            codec.decodeResponse<GraphQLResponse<GetCurrentUserData>>(
                """{"data":null,"errors":[{"message":5}]}""",
                type,
            )
        }
        assertThrows(GraphQLResponseDecodingException::class.java) {
            codec.decodeResponse<GraphQLResponse<GetCurrentUserData>>(
                """{"data":null,"errors":[{"message":null}]}""",
                type,
            )
        }
    }

    @Test
    fun `decodeResponse fails on invalid path shapes`() {
        val type = responseType(GetCurrentUserData::class.java)

        assertThrows(GraphQLResponseDecodingException::class.java) {
            codec.decodeResponse<GraphQLResponse<GetCurrentUserData>>(
                """{"data":null,"errors":[{"message":"bad","path":["viewer",3.5]}]}""",
                type,
            )
        }
        assertThrows(GraphQLResponseDecodingException::class.java) {
            codec.decodeResponse<GraphQLResponse<GetCurrentUserData>>(
                """{"data":null,"errors":[{"message":"bad","path":[true]}]}""",
                type,
            )
        }
        assertThrows(GraphQLResponseDecodingException::class.java) {
            codec.decodeResponse<GraphQLResponse<GetCurrentUserData>>(
                """{"data":null,"errors":[{"message":"bad","path":"viewer"}]}""",
                type,
            )
        }
    }

    @Test
    fun `decodeResponse fails when extensions are not objects`() {
        val type = responseType(GetCurrentUserData::class.java)

        assertThrows(GraphQLResponseDecodingException::class.java) {
            codec.decodeResponse<GraphQLResponse<GetCurrentUserData>>(
                """{"data":null,"extensions":[1,2,3]}""",
                type,
            )
        }
        assertThrows(GraphQLResponseDecodingException::class.java) {
            codec.decodeResponse<GraphQLResponse<GetCurrentUserData>>(
                """{"data":null,"errors":[{"message":"e","extensions":"oops"}]}""",
                type,
            )
        }
    }

    @Test
    fun `decodeResponse fails on malformed JSON and non-object envelopes`() {
        val type = responseType(GetCurrentUserData::class.java)

        val error =
            assertThrows(GraphQLResponseDecodingException::class.java) {
                codec.decodeResponse<GraphQLResponse<GetCurrentUserData>>("""{"data":""", type)
            }
        assertSame(type, error.responseType)

        assertThrows(GraphQLResponseDecodingException::class.java) {
            codec.decodeResponse<GraphQLResponse<GetCurrentUserData>>("""[1,2,3]""", type)
        }
    }

    @Test
    fun `decodeResponse fails for non-parameterized response types`() {
        val rawType = GraphQLResponse::class.java

        val error =
            assertThrows(GraphQLResponseDecodingException::class.java) {
                codec.decodeResponse<GraphQLResponse<Any>>("""{"data":null}""", rawType)
            }

        assertSame(rawType, error.responseType)
    }
}
