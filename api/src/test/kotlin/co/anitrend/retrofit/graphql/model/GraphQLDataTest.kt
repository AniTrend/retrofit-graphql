package co.anitrend.retrofit.graphql.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphQLDataTest {

    @Test
    fun `Absent is a singleton`() {
        assertSame(GraphQLData.Absent, GraphQLData.Absent)
        assertTrue(GraphQLData.Absent is GraphQLData<*>)
    }

    @Test
    fun `Absent is distinct from an explicitly null value`() {
        val presentNull = GraphQLData.Present<Any?>(null)

        assertNotEquals(GraphQLData.Absent, presentNull)
        assertNull(presentNull.value)
    }

    @Test
    fun `Present holds a non-null value`() {
        val present = GraphQLData.Present("octocat")

        assertEquals(GraphQLData.Present("octocat"), present)
        assertEquals("octocat", present.value)
    }

    @Test
    fun `Absent and Present flow through the covariant type`() {
        val absent: GraphQLData<Any> = GraphQLData.Absent
        val present: GraphQLData<Any> = GraphQLData.Present("value")

        assertTrue(absent is GraphQLData.Absent)
        assertTrue(present is GraphQLData.Present)
        assertEquals("value", (present as GraphQLData.Present).value)
    }

    @Test
    fun `exhaustive when distinguishes the three wire states`() {
        val states = listOf<GraphQLData<String?>>(GraphQLData.Absent, GraphQLData.Present(null), GraphQLData.Present("x"))

        val descriptions = states.map { data ->
            when (data) {
                is GraphQLData.Absent -> "missing"
                is GraphQLData.Present -> if (data.value == null) "explicit-null" else "value"
            }
        }

        assertEquals(listOf("missing", "explicit-null", "value"), descriptions)
    }
}
