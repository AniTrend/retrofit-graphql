package co.anitrend.retrofit.graphql.serialization

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Dependency boundary check for `:serialization-api`.
 *
 * `:serialization-api` must expose only the backend-neutral
 * [GraphQLTransportCodec] contract and depend on `:api` alone. This test
 * scans the resolved test runtime classpath (the actual resolved dependency
 * graph, not source text) and fails when a built-in JSON backend artifact
 * appears.
 */
class BackendDependencyBoundaryTest {

    @Test
    fun `serialization-api runtime classpath carries no built-in JSON backend`() {
        val backendArtifacts = backendArtifactsOnClasspath()
        assertTrue(
            "Expected no Gson/kotlinx.serialization artifacts on the :serialization-api test runtime classpath, found: $backendArtifacts",
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
