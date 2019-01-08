package io.github.wax911.retgraph

import io.github.wax911.library.model.body.GraphContainer
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class AppUnitTest {

    @Test
    fun graph_container_func() {
        val emptyContainer = GraphContainer<Any>(null, null)
        assertTrue(emptyContainer.isEmpty())

        val dataContainer = GraphContainer(listOf("mammal", "reptile"), null)
        assertFalse(dataContainer.isEmpty())
    }
}
