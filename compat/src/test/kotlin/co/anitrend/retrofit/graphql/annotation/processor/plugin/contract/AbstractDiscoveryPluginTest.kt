package co.anitrend.retrofit.graphql.annotation.processor.plugin.contract

import co.anitrend.retrofit.graphql.helpers.ResourcesDiscoveryPlugin
import co.anitrend.retrofit.graphql.helpers.logger.TestLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AbstractDiscoveryPluginTest {

    private lateinit var maps: Map<String, String>

    @Before
    fun setUp() {
        val testLogger = TestLogger()
        /** Using resources instead of assets in this test environment */
        val plugin = ResourcesDiscoveryPlugin()
        maps = plugin.startDiscovery(testLogger)
    }

    @Test
    fun `assure can discover from given resource type`() {
        assertTrue(maps.isNotEmpty())
    }

    @Test
    fun `assure discovered size is equal to actual files count`() {
        assertEquals(13, maps.size)
    }

    @Test
    fun `assure multi line contents are loaded properly`() {
        val expected = """
            fragment UserCore on User {    avatarUrl    bio    company    id    status {        createdAt        emoji        message    }    login}
            """.trimIndent()

        val actual = maps["UserCore.graphql"]

        assertEquals(expected, actual)
    }
}