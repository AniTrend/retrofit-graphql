package co.anitrend.retrofit.graphql.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class GraphQLResponseTest {

    private data class Viewer(val login: String)

    @Test
    fun `data defaults to Absent`() {
        val response = GraphQLResponse<Viewer>()
        assertTrue(response.data is GraphQLData.Absent)
        assertTrue(response.errors.isEmpty())
        assertNull(response.extensions)
    }

    @Test
    fun `data carries a present value`() {
        val response = GraphQLResponse(data = GraphQLData.Present(Viewer("octocat")))
        assertEquals(Viewer("octocat"), (response.data as GraphQLData.Present).value)
    }

    @Test
    fun `error message is required and non-null`() {
        val error = GraphQLResponseError(message = "Cannot query field 'logins' on type 'User'.")

        assertEquals("Cannot query field 'logins' on type 'User'.", error.message)
        assertNull(error.locations)
        assertNull(error.path)
        assertNull(error.extensions)
    }

    @Test
    fun `error locations path and extensions are optional`() {
        val error =
            GraphQLResponseError(
                message = "boom",
                locations = listOf(GraphQLResponseError.Location(line = 1, column = 10)),
                path =
                    listOf(
                        GraphQLPathSegment.Field("viewer"),
                        GraphQLPathSegment.Field("repositories"),
                        GraphQLPathSegment.Index(0),
                    ),
                extensions =
                    GraphQLValue.ObjectValue(
                        fields = mapOf("code" to GraphQLValue.StringValue("BAD_USER_INPUT")),
                    ),
            )

        assertEquals(listOf(GraphQLResponseError.Location(line = 1, column = 10)), error.locations)
        assertEquals(
            listOf(
                GraphQLPathSegment.Field("viewer"),
                GraphQLPathSegment.Field("repositories"),
                GraphQLPathSegment.Index(0),
            ),
            error.path,
        )
        assertEquals(GraphQLValue.StringValue("BAD_USER_INPUT"), error.extensions?.fields?.get("code"))
    }

    @Test
    fun `top level extensions use neutral object values`() {
        val response =
            GraphQLResponse(
                data = GraphQLData.Present(Viewer("octocat")),
                extensions =
                    GraphQLValue.ObjectValue(
                        fields =
                            mapOf(
                                "cost" to
                                    GraphQLValue.ObjectValue(
                                        fields =
                                            mapOf(
                                                "requested" to GraphQLValue.NumberValue(BigDecimal("3")),
                                                "throttled" to GraphQLValue.BooleanValue(false),
                                            ),
                                    ),
                            ),
                    ),
            )

        val cost = response.extensions?.fields?.get("cost") as GraphQLValue.ObjectValue
        assertEquals(GraphQLValue.NumberValue(BigDecimal("3")), cost.fields["requested"])
        assertEquals(GraphQLValue.BooleanValue(false), cost.fields["throttled"])
    }

    @Test
    fun `response carries errors alongside absent data`() {
        val response =
            GraphQLResponse<Viewer>(
                data = GraphQLData.Absent,
                errors =
                    listOf(
                        GraphQLResponseError(message = "Unauthorized"),
                        GraphQLResponseError(message = "Rate limited"),
                    ),
            )

        assertEquals(2, response.errors.size)
        assertEquals("Unauthorized", response.errors[0].message)
        assertEquals("Rate limited", response.errors[1].message)
        assertTrue(response.data is GraphQLData.Absent)
    }
}
