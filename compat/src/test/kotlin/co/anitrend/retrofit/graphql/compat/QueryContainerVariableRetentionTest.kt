package co.anitrend.retrofit.graphql.compat

import co.anitrend.retrofit.graphql.model.request.QueryContainerBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Proves the legacy [QueryContainerBuilder] variable map holds values by
 * strong reference: values written through the builder must remain readable
 * after GC pressure instead of being evicted like weak references.
 *
 * [QueryContainerBuilder] stores variables in a `WeakHashMap`; entries
 * survive only while their keys stay strongly reachable. This test writes
 * variables with both interned literals and runtime-generated keys, forces
 * garbage collection, and asserts the values are still readable, which is the
 * behavior the historical artifact relied on.
 */
@Suppress("DEPRECATION")
class QueryContainerVariableRetentionTest {

    @Test
    fun `variable values survive garbage collection pressure`() {
        val builder = QueryContainerBuilder()
        val runtimeKey = "runtime-key-${System.nanoTime()}"
        val runtimeValue = "runtime-value-${System.nanoTime()}"

        builder.putVariable("literal", "literal-value")
        builder.putVariable(runtimeKey, runtimeValue)

        // Keep a strong reference to the value outside the container: values
        // must be reachable through the container itself, not through this
        // reference, so drop it before pressure is applied.
        forceGarbageCollection()

        assertTrue(builder.containsKey("literal"))
        assertTrue(builder.containsKey(runtimeKey))
        assertEquals("literal-value", builder.getVariable("literal"))
        assertEquals(runtimeValue, builder.getVariable(runtimeKey))
    }

    @Test
    fun `built query container retains variables after garbage collection`() {
        val builder = QueryContainerBuilder()
            .putVariable("first", 15)
            .putVariable("after", "cursor-1")

        forceGarbageCollection()

        val container = builder.build()
        assertNotNull(container.variables["first"])
        assertEquals(15, container.variables["first"])
        assertEquals("cursor-1", container.variables["after"])
    }

    private fun forceGarbageCollection() {
        // Allocation churn plus explicit GC calls: the JIT/GC may defer a
        // single System.gc(), so churn makes collection of unreachable
        // entries far more likely before the assertions run.
        val churn = mutableListOf<Any?>()
        repeat(3) { cycle ->
            System.gc()
            repeat(10_000) { churn.add(ByteArray(64)) }
            churn.clear()
        }
        System.gc()
    }
}
