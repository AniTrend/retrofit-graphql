package co.anitrend.retrofit.graphql.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class GraphQLValueTest {

    @Test
    fun `Null is an explicit singleton null`() {
        assertEquals(GraphQLValue.Null, GraphQLValue.Null)
        assertTrue(GraphQLValue.Null is GraphQLValue)
    }

    @Test
    fun `scalar values hold their payloads`() {
        assertEquals(GraphQLValue.BooleanValue(true), GraphQLValue.BooleanValue(true))
        assertEquals(GraphQLValue.StringValue("octocat"), GraphQLValue.StringValue("octocat"))
        assertEquals(
            GraphQLValue.NumberValue(BigDecimal("42.5")),
            GraphQLValue.NumberValue(BigDecimal("42.5")),
        )
    }

    @Test
    fun `nested values preserve the full structure`() {
        val value: GraphQLValue =
            GraphQLValue.ObjectValue(
                fields =
                    mapOf(
                        "viewer" to
                            GraphQLValue.ObjectValue(
                                fields =
                                    mapOf(
                                        "login" to GraphQLValue.StringValue("octocat"),
                                        "repositories" to
                                            GraphQLValue.ListValue(
                                                values =
                                                    listOf(
                                                        GraphQLValue.ObjectValue(
                                                            fields =
                                                                mapOf(
                                                                    "name" to GraphQLValue.StringValue("retrofit-graphql"),
                                                                    "stargazerCount" to GraphQLValue.NumberValue(BigDecimal("1234")),
                                                                ),
                                                        ),
                                                        GraphQLValue.Null,
                                                    ),
                                            ),
                                        "isDeveloper" to GraphQLValue.BooleanValue(true),
                                    ),
                            ),
                        "nothing" to GraphQLValue.Null,
                    ),
            )

        val viewer = (value as GraphQLValue.ObjectValue).fields.getValue("viewer") as GraphQLValue.ObjectValue
        assertEquals(GraphQLValue.StringValue("octocat"), viewer.fields["login"])
        assertEquals(GraphQLValue.BooleanValue(true), viewer.fields["isDeveloper"])
        assertEquals(GraphQLValue.Null, (value as GraphQLValue.ObjectValue).fields["nothing"])

        val repositories = viewer.fields.getValue("repositories") as GraphQLValue.ListValue
        assertEquals(2, repositories.values.size)
        val first = repositories.values[0] as GraphQLValue.ObjectValue
        assertEquals(GraphQLValue.StringValue("retrofit-graphql"), first.fields["name"])
        assertEquals(GraphQLValue.NumberValue(BigDecimal("1234")), first.fields["stargazerCount"])
        assertEquals(GraphQLValue.Null, repositories.values[1])
    }

    @Test
    fun `number values preserve full decimal precision`() {
        val precise = BigDecimal("1234567890.12345678901234567890")
        val number = GraphQLValue.NumberValue(precise)

        assertEquals(precise, number.value)
        assertEquals(0, precise.compareTo(BigDecimal("1234567890.12345678901234567890")))
        assertEquals("1234567890.12345678901234567890", number.value.toPlainString())
    }

    @Test
    fun `number equality is scale sensitive like BigDecimal`() {
        assertEquals(GraphQLValue.NumberValue(BigDecimal("1")), GraphQLValue.NumberValue(BigDecimal("1")))
        assertNotEquals(GraphQLValue.NumberValue(BigDecimal("1.0")), GraphQLValue.NumberValue(BigDecimal("1.00")))
    }

    @Test
    fun `object values are keyed by string`() {
        val objectValue = GraphQLValue.ObjectValue(fields = mapOf("key" to GraphQLValue.StringValue("value")))
        assertEquals(setOf("key"), objectValue.fields.keys)
        assertEquals(GraphQLValue.StringValue("value"), objectValue.fields["key"])
    }
}
