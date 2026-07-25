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

package co.anitrend.retrofit.graphql.codegen.generate

import co.anitrend.retrofit.graphql.codegen.config.SerializationBackend
import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.GraphQLVariableInfo
import co.anitrend.retrofit.graphql.codegen.model.OperationType
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import co.anitrend.retrofit.graphql.codegen.model.SchemaType
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Files

/**
 * Compilation tests that verify generated source code compiles successfully
 * with the Kotlin compiler.
 *
 * Layer C: Generated-source compilation tests. Uses K2JVMCompiler to
 * compile fixture-based generated code.
 *
 * Note: variable and input object compilation tests are not feasible here
 * because the generated code references `GraphQLVariables` which lives in
 * the `:api` module (not on the test classpath). Enum classes are
 * self-contained and can be compiled independently.
 */
class CompilationTest {

    // ---- Enums compile ----

    @Test
    fun `enum with uppercase values compiles with NONE backend`() {
        val schemaIndex = SchemaIndex.from(
            listOf(SchemaType.Enum("Status", listOf("OPEN", "CLOSED", "PENDING"))),
        )
        val source =
            EnumGenerator.generate(
                schemaIndex.enums,
                "com.example",
                SerializationBackend.NONE,
            ).single().toString()

        compileAndAssertOk(source, "com/example/Status.kt")
    }

    @Test
    fun `enum with lowercase values compiles with NONE backend`() {
        val schemaIndex = SchemaIndex.from(
            listOf(SchemaType.Enum("Animal", listOf("dog", "cat", "bird"))),
        )
        val source =
            EnumGenerator.generate(
                schemaIndex.enums,
                "com.example",
                SerializationBackend.NONE,
            ).single().toString()

        compileAndAssertOk(source, "com/example/Animal.kt")
    }

    // ---- Backend imports verification ----

    @Test
    fun `KOTLINX backend emits correct imports`() {
        val schemaIndex = SchemaIndex.EMPTY
        val operation = buildOperation(
            "GetItems",
            listOf(
                GraphQLVariableInfo("first", GraphQLType.Named(name = "Int", nullable = false)),
            ),
        )
        val source =
            VariableClassGenerator.generate(
                operation,
                "com.example",
                emptyMap(),
                schemaIndex,
                SerializationBackend.KOTLINX,
            )!!.toString()

        assertTrue(source.contains("import kotlinx.serialization.Serializable"))
        assertTrue(source.contains("import kotlinx.serialization.SerialName"))
        assertFalse(source.contains("com.google.gson"))
        assertTrue(source.contains("@Serializable"))
    }

    @Test
    fun `GSON backend emits correct imports`() {
        val schemaIndex = SchemaIndex.EMPTY
        val operation = buildOperation(
            "GetItems",
            listOf(
                GraphQLVariableInfo("first", GraphQLType.Named(name = "Int", nullable = false)),
            ),
        )
        val source =
            VariableClassGenerator.generate(
                operation,
                "com.example",
                emptyMap(),
                schemaIndex,
                SerializationBackend.GSON,
            )!!.toString()

        assertTrue(source.contains("import com.google.gson.annotations.SerializedName"))
        assertFalse(source.contains("kotlinx.serialization"))
    }

    @Test
    fun `NONE backend imports no serialization annotations`() {
        val schemaIndex = SchemaIndex.EMPTY
        val operation = buildOperation(
            "GetItems",
            listOf(
                GraphQLVariableInfo("first", GraphQLType.Named(name = "Int", nullable = false)),
            ),
        )
        val source =
            VariableClassGenerator.generate(
                operation,
                "com.example",
                emptyMap(),
                schemaIndex,
                SerializationBackend.NONE,
            )!!.toString()

        assertFalse(source.contains("kotlinx.serialization"))
        assertFalse(source.contains("com.google.gson"))
    }

    // ---- Compilation helpers ----

    private fun compileAndAssertOk(
        source: String,
        relativePath: String,
    ) {
        val tmpDir = Files.createTempDirectory("kct-").toFile()
        tmpDir.deleteOnExit()
        try {
            val sourceDir = File(tmpDir, "sources").apply { mkdirs() }
            val outputDir = File(tmpDir, "output").apply { mkdirs() }

            val sourceFile = File(sourceDir, relativePath)
            sourceFile.parentFile?.mkdirs()
            sourceFile.writeText(source)

            val classpath = System.getProperty("java.class.path")
            val errorStream = ByteArrayOutputStream()
            val exitCode = K2JVMCompiler().exec(
                PrintStream(errorStream),
                "-classpath", classpath,
                "-d", outputDir.absolutePath,
                sourceDir.absolutePath,
            )

            assertEquals(
                "Compilation failed: ${errorStream.toString().take(500)}",
                ExitCode.OK,
                exitCode,
            )
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    private fun buildOperation(
        name: String,
        variables: List<GraphQLVariableInfo> = emptyList(),
    ): GraphQLOperationInfo {
        return GraphQLOperationInfo(
            name = name,
            type = OperationType.QUERY,
            document = "query $name",
            sourceFile = "$name.graphql",
            variables = variables,
        )
    }
}
