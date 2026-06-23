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

package co.anitrend.retrofit.graphql.codegen

import co.anitrend.retrofit.graphql.codegen.generate.DocumentGenerator
import co.anitrend.retrofit.graphql.codegen.generate.EnumGenerator
import co.anitrend.retrofit.graphql.codegen.generate.HashConstantsGenerator
import co.anitrend.retrofit.graphql.codegen.generate.InputObjectGenerator
import co.anitrend.retrofit.graphql.codegen.generate.OperationConstantsGenerator
import co.anitrend.retrofit.graphql.codegen.generate.OperationRequestGenerator
import co.anitrend.retrofit.graphql.codegen.generate.RegistryGenerator
import co.anitrend.retrofit.graphql.codegen.generate.VariableClassGenerator
import co.anitrend.retrofit.graphql.codegen.model.GraphQLFragmentInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import co.anitrend.retrofit.graphql.codegen.parser.GraphQLDocumentParser
import co.anitrend.retrofit.graphql.codegen.parser.SchemaParser
import co.anitrend.retrofit.graphql.codegen.resolve.FragmentResolver
import co.anitrend.retrofit.graphql.codegen.resolve.FragmentVariablePropagator
import co.anitrend.retrofit.graphql.codegen.validate.GraphQLTypeUsageValidator
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Gradle task that parses .graphql operation files and generates Kotlin source code:
 * - GraphQLOperations (operation name constants)
 * - GraphQLDocuments (document text constants)
 * - GraphQLHashes (APQ hash constants)
 * - GeneratedGraphQLRegistry (registry implementing GraphQLDocumentRegistry)
 * - Per-operation variable classes (XxxVariables)
 * - Input object classes from schema
 * - Enum classes from schema
 * - Per-operation request helper objects
 */
abstract class GenerateGraphQLSourcesTask : DefaultTask() {
    @get:Input
    abstract val packageName: Property<String>

    @get:InputFiles
    abstract val operationsDir: ConfigurableFileCollection

    @get:Optional
    @get:InputFile
    abstract val schemaFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val generateOperationConstants: Property<Boolean>

    @get:Input
    abstract val generateDocuments: Property<Boolean>

    @get:Input
    abstract val generateHashes: Property<Boolean>

    @get:Input
    abstract val generateVariables: Property<Boolean>

    @get:Input
    abstract val scalarMappings: MapProperty<String, String>

    @TaskAction
    fun generate() {
        val parser = GraphQLDocumentParser()
        val resolver = FragmentResolver()
        val propagator = FragmentVariablePropagator()

        val graphqlFiles = operationsDir.files.filter { it.extension == "graphql" }

        if (graphqlFiles.isEmpty()) {
            logger.info("No .graphql files found in ${operationsDir.asPath}")
            return
        }

        // Parse all operations and fragments from all .graphql files
        val allOperations = mutableListOf<GraphQLOperationInfo>()
        val allFragments = mutableMapOf<String, GraphQLFragmentInfo>()

        for (file in graphqlFiles) {
            logger.info("Parsing ${file.name}")
            allOperations.addAll(parser.parseOperations(file))
            parser.parseFragments(file).forEach { fragment ->
                if (allFragments.containsKey(fragment.name)) {
                    throw IllegalArgumentException(
                        "Duplicate fragment definition '${fragment.name}' " +
                            "found in ${file.name}. Fragment names must be unique.",
                    )
                }
                allFragments[fragment.name] = fragment
            }
        }

        // Detect duplicate operation names
        val nameCounts = allOperations.groupingBy { it.name }.eachCount()
        val duplicates = nameCounts.filter { it.value > 1 }.keys
        if (duplicates.isNotEmpty()) {
            throw IllegalArgumentException(
                "Duplicate operation names found: ${duplicates.joinToString(", ")}. " +
                    "Operation names must be unique across all .graphql files.",
            )
        }

        // Propagate fragment variable usages into consuming operations
        val propagationResult = propagator.propagate(allOperations, allFragments)
        propagationResult.warnings.forEach { warning -> logger.warn(warning) }

        // Flatten fragments into operations
        val resolvedOperations = resolver.resolve(propagationResult.operations, allFragments)

        // Parse schema if available
        val scalarMap = scalarMappings.orNull ?: emptyMap()
        val schemaIndex =
            if (schemaFile.isPresent && schemaFile.get().asFile.exists()) {
                val schemaParser = SchemaParser()
                SchemaIndex.from(schemaParser.parse(schemaFile.get().asFile))
            } else {
                if (generateVariables.get()) {
                    logger.warn(
                        "generateVariables is enabled but no schema file is set. " +
                            "Input object and enum generation will be skipped. " +
                            "Variable classes will use scalar mappings only.",
                    )
                }
                SchemaIndex.EMPTY
            }

        // Generate source files
        val pkg = packageName.get()
        val outputDirectory = outputDir.get().asFile

        if (generateOperationConstants.get()) {
            writeFile(
                OperationConstantsGenerator.generate(resolvedOperations, pkg),
                outputDirectory,
            )
        }

        if (generateDocuments.get()) {
            writeFile(
                DocumentGenerator.generate(resolvedOperations, pkg),
                outputDirectory,
            )
        }

        if (generateHashes.get()) {
            writeFile(
                HashConstantsGenerator.generate(resolvedOperations, pkg),
                outputDirectory,
            )
        }

        // Registry is always generated (needed by the runtime)
        writeFile(
            RegistryGenerator.generate(resolvedOperations, pkg),
            outputDirectory,
        )

        // Variable / input / enum / request helper generation
        if (generateVariables.get()) {
            generateVariableArtifacts(
                resolvedOperations,
                schemaIndex,
                scalarMap,
                pkg,
                outputDirectory,
            )
        }

        logger.lifecycle(
            "Generated GraphQL sources for ${resolvedOperations.size} operation(s) " +
                "in package '$pkg'",
        )
    }

    private fun generateVariableArtifacts(
        operations: List<GraphQLOperationInfo>,
        schemaIndex: SchemaIndex,
        scalarMap: Map<String, String>,
        pkg: String,
        outputDirectory: File,
    ) {
        // Validate unknown scalars in variables
        GraphQLTypeUsageValidator.validate(operations, schemaIndex, scalarMap)

        // Generate per-operation variable classes
        operations.forEach { operation ->
            val varFile =
                VariableClassGenerator.generate(
                    operation,
                    pkg,
                    scalarMap,
                    schemaIndex,
                )
            if (varFile != null) {
                writeFile(varFile, outputDirectory)
                logger.info("Generated ${operation.name}Variables")
            }
        }

        // Generate input object classes from schema
        val inputObjects = schemaIndex.inputObjects
        if (inputObjects.isNotEmpty()) {
            InputObjectGenerator.generate(inputObjects, pkg, scalarMap, schemaIndex)
                .forEach { writeFile(it, outputDirectory) }
            logger.info("Generated ${inputObjects.size} input object class(es)")
        }

        // Generate enum classes from schema
        val enums = schemaIndex.enums
        if (enums.isNotEmpty()) {
            EnumGenerator.generate(enums, pkg).forEach { writeFile(it, outputDirectory) }
            logger.info("Generated ${enums.size} enum class(es)")
        }

        // Generate per-operation request helpers
        operations.forEach { operation ->
            val reqFile =
                OperationRequestGenerator.generate(
                    operation,
                    pkg,
                    scalarMap,
                    schemaIndex,
                )
            writeFile(reqFile, outputDirectory)
            logger.info("Generated ${operation.name} request helper")
        }
    }

    private fun writeFile(
        fileSpec: com.squareup.kotlinpoet.FileSpec,
        outputDirectory: File,
    ) {
        fileSpec.writeTo(outputDirectory)
    }
}
