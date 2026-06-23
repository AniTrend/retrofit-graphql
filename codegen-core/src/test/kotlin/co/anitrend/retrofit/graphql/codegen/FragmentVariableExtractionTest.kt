package co.anitrend.retrofit.graphql.codegen

import co.anitrend.retrofit.graphql.codegen.parser.GraphQLDocumentParser
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class FragmentVariableExtractionTest {

    private val parser = GraphQLDocumentParser()

    private fun createTempFile(content: String): File {
        val temp = File.createTempFile("test", ".graphql")
        temp.writeText(content)
        temp.deleteOnExit()
        return temp
    }

    @Test
    fun `should extract single variable usage from fragment field argument`() {
        val file = createTempFile(
            "fragment Status on User { name age(format: \$format) }",
        )

        val fragments = parser.parseFragments(file)

        assertEquals(1, fragments.size)
        assertEquals(listOf("format"), fragments.single().variableUsages)
    }

    @Test
    fun `should extract multiple variable usages from fragment`() {
        val file = createTempFile(
            "fragment Profile on User { name age(format: \$format) avatar(size: \$size) }",
        )

        val fragments = parser.parseFragments(file)

        assertEquals(1, fragments.size)
        val usages = fragments.single().variableUsages
        assertTrue("Should contain 'format'", "format" in usages)
        assertTrue("Should contain 'size'", "size" in usages)
        assertEquals(2, usages.size)
    }

    @Test
    fun `should extract variable from directive argument`() {
        val file = createTempFile(
            "fragment Status on User { name @include(if: \$showName) }",
        )

        val fragments = parser.parseFragments(file)

        assertEquals(1, fragments.size)
        assertEquals(listOf("showName"), fragments.single().variableUsages)
    }

    @Test
    fun `should extract variable from nested input object argument`() {
        val file = createTempFile(
            "fragment Filter on User { search(filter: { name: \$filterName }) }",
        )

        val fragments = parser.parseFragments(file)

        assertEquals(1, fragments.size)
        assertEquals(listOf("filterName"), fragments.single().variableUsages)
    }

    @Test
    fun `should return empty list when fragment has no variable usages`() {
        val file = createTempFile(
            "fragment Status on User { name age }",
        )

        val fragments = parser.parseFragments(file)

        assertEquals(1, fragments.size)
        assertTrue("Variable usages should be empty", fragments.single().variableUsages.isEmpty())
    }

    @Test
    fun `should extract variables from multiple fragments in same file`() {
        val file = createTempFile(
            """
            fragment Status on User { name age(format: ${'$'}format) }
            fragment Profile on User { avatar(size: ${'$'}size) }
            """.trimIndent(),
        )

        val fragments = parser.parseFragments(file)

        assertEquals(2, fragments.size)
        val status = fragments.first { it.name == "Status" }
        val profile = fragments.first { it.name == "Profile" }
        assertEquals(listOf("format"), status.variableUsages)
        assertEquals(listOf("size"), profile.variableUsages)
    }
}