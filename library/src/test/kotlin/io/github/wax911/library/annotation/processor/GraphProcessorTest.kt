package io.github.wax911.library.annotation.processor

import io.github.wax911.library.annotation.processor.contract.AbstractGraphProcessor
import io.github.wax911.library.helpers.ResourcesDiscoveryPlugin
import io.github.wax911.library.helpers.annotations.MockAnnotationStubs
import io.github.wax911.library.helpers.logger.TestLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import kotlin.reflect.full.functions

class GraphProcessorTest {

    private lateinit var processor: AbstractGraphProcessor

    @Before
    fun setUp() {
        val testLogger = TestLogger()
        /** Using resources instead of assets in this test environment */
        val plugin = ResourcesDiscoveryPlugin()
        processor = GraphProcessor(
            discoveryPlugin = plugin,
            logger = testLogger
        )
    }

    @Test
    fun `assert all annotations can be reference valid graphql files`() {
        val functions = MockAnnotationStubs::class.functions
        val graphs = functions.map {
            processor.getQuery(it.annotations.toTypedArray())
        }
        assertEquals(9, graphs.size)
    }

    @Test
    fun `given an annotation for finding an issue, assure that a query can be resolved`() {
        val annotations = MockAnnotationStubs::findIssueById.annotations
        val expected = """
            query FindIssueID(            "\$"owner            : String!,             "\$"name            : String!,             "\$"issueId            : Int!) {    repository(owner:            "\$"owner            , name:            "\$"name            ) {        issue(number:            "\$"issueId            ) {            id        }    }}
        """.trimIndent()
        val actual = processor.getQuery(annotations.toTypedArray())

        assertNotNull(expected, actual)
    }
}