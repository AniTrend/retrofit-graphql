package co.anitrend.retrofit.graphql.api

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Dependency boundary check for `:api`.
 *
 * `:api` must contain only backend-neutral protocol/operation/registry
 * contracts and standard Kotlin/Java types: no Gson, no kotlinx.serialization,
 * no Parcelize, no Android framework types. This test scans the resolved test
 * runtime classpath (the actual resolved dependency graph, not source text)
 * and fails when a built-in JSON backend artifact appears.
 */
class BackendDependencyBoundaryTest {

    @Test
    fun `api runtime classpath carries no built-in JSON backend`() {
        val backendArtifacts = backendArtifactsOnClasspath()
        assertTrue(
            "Expected no Gson/kotlinx.serialization artifacts on the :api test runtime classpath, found: $backendArtifacts",
            backendArtifacts.isEmpty(),
        )
    }

    companion object {
        fun backendArtifactsOnClasspath(): List<String> =
            System.getProperty("java.class.path")
                .split(File.pathSeparator)
                .filter { entry ->
                    val lower = entry.lowercase()
                    lower.contains("gson") || lower.contains("kotlinx-serialization")
                }
    }
}
