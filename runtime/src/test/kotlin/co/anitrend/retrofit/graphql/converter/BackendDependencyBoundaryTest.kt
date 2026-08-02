package co.anitrend.retrofit.graphql.converter

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Dependency boundary check for `:runtime`.
 *
 * `:runtime` must contain only the explicit-codec converter factory and
 * converters plus discovery contracts: no Gson, no kotlinx.serialization.
 * This test scans the resolved test runtime classpath (the actual resolved
 * dependency graph, not source text) and fails when a built-in JSON backend
 * artifact appears. The legacy backend path is owned by `:compat`.
 */
class BackendDependencyBoundaryTest {

    @Test
    fun `runtime runtime classpath carries no built-in JSON backend`() {
        val backendArtifacts = backendArtifactsOnClasspath()
        assertTrue(
            "Expected no Gson/kotlinx.serialization artifacts on the :runtime test runtime classpath, found: $backendArtifacts",
            backendArtifacts.isEmpty(),
        )
    }

    private fun backendArtifactsOnClasspath(): List<String> =
        System.getProperty("java.class.path")
            .split(File.pathSeparator)
            .filter { entry ->
                val lower = entry.lowercase()
                lower.contains("gson") || lower.contains("kotlinx-serialization")
            }
}
