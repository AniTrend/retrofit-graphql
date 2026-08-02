package co.anitrend.retrofit.graphql.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphQLPathSegmentTest {

    @Test
    fun `field segments hold the field name`() {
        val segment = GraphQLPathSegment.Field("viewer")
        assertEquals("viewer", segment.name)
        assertEquals(GraphQLPathSegment.Field("viewer"), GraphQLPathSegment.Field("viewer"))
    }

    @Test
    fun `index segments hold the zero-based position`() {
        val segment = GraphQLPathSegment.Index(3)
        assertEquals(3, segment.value)
        assertEquals(GraphQLPathSegment.Index(3), GraphQLPathSegment.Index(3))
        assertNotEquals(GraphQLPathSegment.Index(3), GraphQLPathSegment.Index(4))
    }

    @Test
    fun `field and index segments are distinct kinds`() {
        val field = GraphQLPathSegment.Field("repositories")
        val index = GraphQLPathSegment.Index(0)

        assertTrue(field is GraphQLPathSegment.Field)
        assertTrue(index is GraphQLPathSegment.Index)
        assertNotEquals(field, index)
        assertNotEquals(index, field)
    }

    @Test
    fun `path reads as a mixed sequence of segments`() {
        val path =
            listOf(
                GraphQLPathSegment.Field("viewer"),
                GraphQLPathSegment.Field("repositories"),
                GraphQLPathSegment.Index(0),
            )

        assertEquals(3, path.size)
        assertEquals(GraphQLPathSegment.Field("viewer"), path[0])
        assertEquals(GraphQLPathSegment.Field("repositories"), path[1])
        assertEquals(GraphQLPathSegment.Index(0), path[2])
    }
}
