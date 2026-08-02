/**
 * Copyright 2026 AniTrend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.anitrend.retrofit.graphql.codegen.generate.helper

import com.squareup.kotlinpoet.FileSpec
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.junit.Assert.assertEquals
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path

/**
 * Compiles generated Kotlin [FileSpec] objects at test time and provides a
 * [ClassLoader] for loading the compiled classes.
 *
 * This helper uses the embedded [K2JVMCompiler] already available as a
 * test dependency. For classes that implement interfaces from the `:api`
 * module, stub interfaces/types are automatically generated in the same
 * compilation unit so the generated code compiles against valid supertypes.
 *
 * @since 1.0
 */
object CompilationHelper {

    /**
     * Result of a successful compilation.
     *
     * Implements [AutoCloseable] to clean up the temporary output directory.
     * Prefer `.use { }` blocks for automatic cleanup.
     *
     * @property classLoader A [URLClassLoader] that can load the compiled classes.
     * @property outputDir The temporary directory containing compiled class files.
     */
    class CompilationResult(
        val classLoader: URLClassLoader,
        private val outputDir: Path,
        private val sourceTmpDir: File,
    ) : AutoCloseable {

        override fun close() {
            try { classLoader.close() } catch (_: Exception) {}
            try { outputDir.toFile().deleteRecursively() } catch (_: Exception) {}
            try { sourceTmpDir.deleteRecursively() } catch (_: Exception) {}
        }
    }

    /**
     * Compiles the given set of [fileSpecs] and returns a [CompilationResult]
     * with a classloader for the compiled classes.
     *
     * For generated code that implements `co.anitrend.retrofit.graphql.model.GraphQLVariables`,
     * a stub interface is automatically included in the compilation unit. This
     * ensures compilation succeeds even though `:api` is not on the test classpath.
     *
     * Set [includeGraphQLStubs] to also generate stubs for `GraphContainer<T>`
     * and `GraphError` (needed for response envelope round-trip tests).
     *
     * @param fileSpecs The KotlinPoet [FileSpec] objects to compile.
     * @param includeGraphQLVariablesStub Whether to include a stub `GraphQLVariables`
     *   interface. Set to `true` when compiling variable classes or input objects.
     * @param includeGraphQLStubs Whether to include stub `GraphContainer<T>` and
     *   `GraphError` classes. Set to `true` for response envelope round-trip tests.
     * @return [CompilationResult] with the classloader for the compiled classes.
     */
    fun compile(
        fileSpecs: List<FileSpec>,
        includeGraphQLVariablesStub: Boolean = false,
        includeGraphQLStubs: Boolean = false,
    ): CompilationResult {
        val tmpDir = Files.createTempDirectory("kct-phase2-").toFile()
        val sourceDir = File(tmpDir, "sources").apply { mkdirs() }
        val outputDir = Files.createTempDirectory("kct-output-")

        try {
            // Write generated files to source directory
            for (fileSpec in fileSpecs) {
                val packagePath = fileSpec.packageName.replace('.', '/')
                val fileDir = File(sourceDir, packagePath).apply { mkdirs() }
                val sourceFile = File(fileDir, "${fileSpec.name}.kt")
                sourceFile.writeText(fileSpec.toString())
            }

            // Optionally include stub GraphQL model types
            if (includeGraphQLVariablesStub || includeGraphQLStubs) {
                writeGraphQLModelStubs(sourceDir, includeGraphQLStubs)
            }

            val classpath = System.getProperty("java.class.path")
            val errorStream = ByteArrayOutputStream()
            val exitCode = K2JVMCompiler().exec(
                PrintStream(errorStream),
                "-classpath", classpath,
                "-d", outputDir.toAbsolutePath().toString(),
                sourceDir.absolutePath,
            )

            assertEquals(
                "Compilation failed: ${errorStream.toString().take(1000)}",
                ExitCode.OK,
                exitCode,
            )

            val classLoader = URLClassLoader(
                arrayOf(outputDir.toUri().toURL()),
                CompilationHelper::class.java.classLoader,
            )

            // Schedule cleanup for catastrophic failures, but normally close() handles it
            tmpDir.deleteOnExit()
            outputDir.toFile().deleteOnExit()

            return CompilationResult(
                classLoader = classLoader,
                outputDir = outputDir,
                sourceTmpDir = tmpDir,
            )
        } catch (e: Exception) {
            try { tmpDir.deleteRecursively() } catch (_: Exception) {}
            try { outputDir.toFile().deleteRecursively() } catch (_: Exception) {}
            throw e
        }
    }

    /**
     * Convenience overload that compiles a single [FileSpec].
     */
    fun compileSingle(
        fileSpec: FileSpec,
        includeGraphQLVariablesStub: Boolean = false,
        includeGraphQLStubs: Boolean = false,
    ): CompilationResult = compile(listOf(fileSpec), includeGraphQLVariablesStub, includeGraphQLStubs)

    /**
     * Writes stub GraphQL model types to the source directory for compilation.
     */
    private fun writeGraphQLModelStubs(sourceDir: File, includeGraphQLStubs: Boolean) {
        val stubDir = File(sourceDir, "co/anitrend/retrofit/graphql/model").apply { mkdirs() }

        val variablesStubFile = File(stubDir, "GraphQLVariables.kt")
        variablesStubFile.writeText(
            """
            package co.anitrend.retrofit.graphql.model

            /** Stub interface for test compilation only. */
            interface GraphQLVariables

            /** Empty singleton for tests that need a concrete instance. */
            object EmptyGraphQLVariables : GraphQLVariables

            /** Stub operation contract for test compilation only. */
            interface GraphQLOperation<TVariables : GraphQLVariables> {
                val name: String
                val document: String
                val sha256Hash: String
            }

            /** Stub convenience contract for no-variable operations. */
            interface GraphQLNoVarOperation : GraphQLOperation<EmptyGraphQLVariables>
            """.trimIndent(),
        )

        val requestDir = File(stubDir, "request").apply { mkdirs() }
        val requestStubFile = File(requestDir, "GraphQLOperationRequest.kt")
        requestStubFile.writeText(
            """
            package co.anitrend.retrofit.graphql.model.request

            import co.anitrend.retrofit.graphql.model.GraphQLVariables

            /** Stub backend-neutral request for test compilation only. */
            data class GraphQLOperationRequest<TVariables : GraphQLVariables>(
                val query: String,
                val operationName: String,
                val variables: TVariables? = null,
            )
            """.trimIndent(),
        )

        if (includeGraphQLStubs) {
            val containerStubFile = File(stubDir, "GraphQLModel.kt")
            containerStubFile.writeText(
                """
                package co.anitrend.retrofit.graphql.model

                import com.google.gson.annotations.SerializedName

                /**
                 * Stub GraphQL response container for test compilation only.
                 * Mirrors the real [co.anitrend.retrofit.graphql.model.body.GraphContainer].
                 */
                data class GraphContainer<T>(
                    @SerializedName("data") val data: T? = null,
                    @SerializedName("errors") val errors: List<GraphError>? = null,
                    @SerializedName("extensions") val extensions: Map<Any, Any>? = null,
                )

                /**
                 * Stub GraphQL error for test compilation only.
                 * Mirrors the real [co.anitrend.retrofit.graphql.model.attribute.GraphError].
                 */
                data class GraphError(
                    @SerializedName("message") val message: String? = null,
                    @SerializedName("path") val path: List<Any>? = null,
                    @SerializedName("locations") val locations: List<Location>? = null,
                    @SerializedName("extensions") val extensions: Map<String, Any?>? = null,
                )

                /** Stub location for test compilation only. */
                data class Location(
                    @SerializedName("line") val line: Int? = null,
                    @SerializedName("column") val column: Int? = null,
                )
                """.trimIndent(),
            )

            val attributeDir = File(sourceDir, "co/anitrend/retrofit/graphql/model/attribute").apply { mkdirs() }
            val errorStubFile = File(attributeDir, "GraphError.kt")
            errorStubFile.writeText(
                """
                package co.anitrend.retrofit.graphql.model.attribute

                import com.google.gson.annotations.SerializedName

                /** Stub GraphError for test compilation, with attribute package path. */
                data class GraphError(
                    @SerializedName("message") val message: String? = null,
                    @SerializedName("path") val path: List<Any>? = null,
                    @SerializedName("locations") val locations: List<Location>? = null,
                    @SerializedName("extensions") val extensions: Map<String, Any?>? = null,
                )

                /** Stub location. */
                data class Location(
                    @SerializedName("line") val line: Int? = null,
                    @SerializedName("column") val column: Int? = null,
                )
                """.trimIndent(),
            )
        }
    }
}
