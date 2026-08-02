package co.anitrend.retrofit.graphql.compat

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Positive dependency boundary check for `:compat`.
 *
 * `:compat` is the only deprecated legacy implementation module and owns the
 * legacy backend path: its resolved test runtime classpath must contain Gson
 * and kotlinx.serialization artifacts (used by the moved legacy surface),
 * while the neutral modules are asserted free of them by their own boundary
 * tests.
 */
class CompatOwnsLegacyBackendTest {

    @Test
    fun `compat runtime classpath carries the legacy Gson and kotlinx artifacts`() {
        val classpath = System.getProperty("java.class.path").split(File.pathSeparator)
        val lowerEntries = classpath.map { it.lowercase() }

        assertTrue(
            "Expected Gson on the :compat test runtime classpath, found: ${classpath.take(5)}",
            lowerEntries.any { it.contains("gson") && it.endsWith(".jar") },
        )
        assertTrue(
            "Expected kotlinx-serialization on the :compat test runtime classpath, found: ${classpath.take(5)}",
            lowerEntries.any { it.contains("kotlinx-serialization") },
        )
    }
}
