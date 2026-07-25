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

import co.anitrend.retrofit.graphql.codegen.config.SerializationBackend
import co.anitrend.retrofit.graphql.codegen.generate.DocumentGenerator
import co.anitrend.retrofit.graphql.codegen.generate.EnumGenerator
import co.anitrend.retrofit.graphql.codegen.generate.HashConstantsGenerator
import co.anitrend.retrofit.graphql.codegen.generate.InputObjectGenerator
import co.anitrend.retrofit.graphql.codegen.generate.OperationConstantsGenerator
import co.anitrend.retrofit.graphql.codegen.generate.OperationRequestGenerator
import co.anitrend.retrofit.graphql.codegen.generate.RegistryGenerator
import co.anitrend.retrofit.graphql.codegen.generate.ResponseModelGenerator
import co.anitrend.retrofit.graphql.codegen.generate.VariableClassGenerator
import co.anitrend.retrofit.graphql.codegen.model.GraphQLFragmentInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.OperationType
import co.anitrend.retrofit.graphql.codegen.model.ResponseSelectionSet
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import co.anitrend.retrofit.graphql.codegen.parser.GraphQLDocumentParser
import co.anitrend.retrofit.graphql.codegen.parser.ResponseSelectionParser
import co.anitrend.retrofit.graphql.codegen.parser.SchemaParser
import co.anitrend.retrofit.graphql.codegen.resolve.FragmentResolver
import co.anitrend.retrofit.graphql.codegen.resolve.FragmentVariablePropagator
import co.anitrend.retrofit.graphql.codegen.schema.SchemaCompiler
import co.anitrend.retrofit.graphql.codegen.schema.TypenameInjector
import co.anitrend.retrofit.graphql.codegen.validate.GraphQLTypeUsageValidator
import com.squareup.kotlinpoet.FileSpec
import graphql.language.OperationDefinition
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
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
 *
 * Task is cacheable: output depends solely on declared inputs. Input files
 * are sorted before processing to ensure deterministic name allocation and
 * byte-identical output across environments.
 */
@CacheableTask
abstract class GenerateGraphQLSourcesTask : DefaultTask() {
    @get:Input
    abstract val packageName: Property<String>

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    abstract val operationsDir: ConfigurableFileCollection

    @get:PathSensitive(PathSensitivity.RELATIVE)
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
    abstract val generateResponses: Property<Boolean>

    @get:Input
    abstract val serializationBackend: Property<SerializationBackend>

    @get:Input
    abstract val scalarMappings: MapProperty<String, String>

    @TaskAction
    fun generate() {
        val parser = GraphQLDocumentParser()
        val resolver = FragmentResolver()
        val propagator = FragmentVariablePropagator()

        // Sort by canonical path for deterministic processing order across
        // environments and filesystems. This is critical because GraphNameAllocator
        // (backed by KotlinPoet NameAllocator) is order-dependent: the first
        // encountered name wins, and collision suffixes vary by encounter order.
        val graphqlFiles = operationsDir.files
            .filter { it.extension == "graphql" }
            .sortedBy { it.canonicalPath }

        val outputDirectory = outputDir.get().asFile

        if (graphqlFiles.isEmpty()) {
            outputDirectory.deleteRecursively()
            outputDirectory.mkdirs()
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
                val result = schemaParser.parseWithRootTypes(schemaFile.get().asFile)
                SchemaIndex.from(
                    types = result.types,
                    queryTypeName = result.queryTypeName,
                    mutationTypeName = result.mutationTypeName,
                    subscriptionTypeName = result.subscriptionTypeName,
                )
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

        // Inject __typename into executable documents for abstract types
        val typedOps = if (generateResponses.get() && schemaIndex != SchemaIndex.EMPTY) {
            val injector = TypenameInjector(schemaIndex)
            resolvedOperations.map { op ->
                val transformedDoc = injector.injectTypename(op.document, op.type.toGraphQLOperation())
                op.copy(document = transformedDoc)
            }
        } else {
            resolvedOperations
        }

        // Validate operations against the schema if both are available
        if (schemaIndex != SchemaIndex.EMPTY) {
            val compiler = SchemaCompiler(scalarMap)
            val schemaIdl = schemaFile.get().asFile.readText()
            val compiledSchema = compiler.compile(schemaIdl)

            val validationErrors = mutableListOf<String>()
            for (op in typedOps) {
                val result = compiler.validate(compiledSchema, op.document)
                if (!result.isValid) {
                    validationErrors.add(
                        "Operation '${op.name}': ${result.errors.joinToString("; ")}",
                    )
                }
            }

            if (validationErrors.isNotEmpty()) {
                throw GradleException(
                    "Schema validation failed for ${validationErrors.size} operation(s):\n" +
                        validationErrors.joinToString("\n"),
                )
            } else {
                logger.info("Schema validation passed for ${typedOps.size} operation(s)")
            }
        }

        // Generate source files
        val pkg = packageName.get()

        // Resolve and validate serialization backend
        val backend = resolveBackend()

        val generatedFiles = mutableListOf<FileSpec>()

        if (generateOperationConstants.get()) {
            generatedFiles.add(
                OperationConstantsGenerator.generate(typedOps, pkg),
            )
        }

        if (generateDocuments.get()) {
            generatedFiles.add(
                DocumentGenerator.generate(typedOps, pkg),
            )
        }

        if (generateHashes.get()) {
            generatedFiles.add(
                HashConstantsGenerator.generate(typedOps, pkg),
            )
        }

        // Registry is always generated (needed by the runtime)
        generatedFiles.add(
            RegistryGenerator.generate(typedOps, pkg),
        )

        // Variable / input / enum / request helper generation
        if (generateVariables.get()) {
            generatedFiles.addAll(
                generateVariableArtifacts(
                    typedOps,
                    schemaIndex,
                    scalarMap,
                    pkg,
                    backend,
                ),
            )
        }

        // Response model generation
        if (generateResponses.get()) {
            if (schemaIndex == SchemaIndex.EMPTY) {
                throw GradleException(
                    "generateResponses is enabled but no valid schema file is set. " +
                        "Provide a schema file to generate response models, " +
                            "or set generateResponses = false.",
                )
            } else {
                generatedFiles.addAll(
                    generateResponseArtifacts(
                        typedOps,
                        schemaIndex,
                        scalarMap,
                        pkg,
                        backend,
                    ),
                )
            }
        }

        // Clean stale output only after parsing, schema validation, backend
        // resolution, type usage validation, and response validation succeed.
        outputDirectory.deleteRecursively()
        outputDirectory.mkdirs()

        generatedFiles.forEach { fileSpec ->
            writeFile(fileSpec, outputDirectory)
        }

        logger.lifecycle(
            "Generated GraphQL sources for ${typedOps.size} operation(s) " +
                "in package '$pkg'",
        )
    }

    /**
     * Resolves the effective [SerializationBackend] from the Gradle property,
     * applying backward-compatible auto-selection.
     *
     * - If [generateResponses] is `true` and the configured backend is [SerializationBackend.NONE],
     *   auto-selects [SerializationBackend.KOTLINX] with a warning (backward compatibility).
     * - Otherwise returns the configured backend.
     */
    private fun resolveBackend(): SerializationBackend {
        val backend = serializationBackend.get()
        if (generateResponses.get() && backend == SerializationBackend.NONE) {
            logger.warn(
                "generateResponses is enabled but serializationBackend is NONE. " +
                    "Auto-selecting KOTLINX for backward compatibility. " +
                    "Explicitly set serializationBackend = KOTLINX to silence this warning.",
            )
            return SerializationBackend.KOTLINX
        }
        return backend
    }

    private fun generateVariableArtifacts(
        operations: List<GraphQLOperationInfo>,
        schemaIndex: SchemaIndex,
        scalarMap: Map<String, String>,
        pkg: String,
        backend: SerializationBackend,
    ): List<FileSpec> {
        val fileSpecs = mutableListOf<FileSpec>()

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
                    backend,
                )
            if (varFile != null) {
                fileSpecs.add(varFile)
                logger.info("Generated ${operation.name}Variables")
            }
        }

        // Generate input object classes from schema
        val inputObjects = schemaIndex.inputObjects
        if (inputObjects.isNotEmpty()) {
            fileSpecs.addAll(InputObjectGenerator.generate(inputObjects, pkg, scalarMap, schemaIndex, backend))
            logger.info("Generated ${inputObjects.size} input object class(es)")
        }

        // Generate enum classes from schema
        fileSpecs.addAll(generateEnumArtifacts(schemaIndex, pkg, "variables", backend))

        // Generate per-operation request helpers
        operations.forEach { operation ->
            val reqFile =
                OperationRequestGenerator.generate(
                    operation,
                    pkg,
                    scalarMap,
                    schemaIndex,
                )
            fileSpecs.add(reqFile)
            logger.info("Generated ${operation.name} request helper")
        }

        return fileSpecs
    }

    private fun generateResponseArtifacts(
        operations: List<GraphQLOperationInfo>,
        schemaIndex: SchemaIndex,
        scalarMap: Map<String, String>,
        pkg: String,
        backend: SerializationBackend,
    ): List<FileSpec> {
        val selectionParser = ResponseSelectionParser(schemaIndex)
        val modelGenerator = ResponseModelGenerator(schemaIndex, scalarMap, backend)
        val parsedOperations = operations.map { operation ->
            operation to selectionParser.parse(operation)
        }

        // Per-operation GSON validation: allow concrete-only operations,
        // fail for operations with interface/union response paths
        if (backend == SerializationBackend.GSON) {
            val allViolations = mutableListOf<String>()
            for ((operation, selectionSet) in parsedOperations) {
                val abstractPaths = findAbstractTypePaths(selectionSet, schemaIndex)
                if (abstractPaths.isNotEmpty()) {
                    allViolations.add(
                        "Operation '${operation.name}' has abstract type(s) at response path(s): " +
                            abstractPaths.joinToString(", ") { (type, path) ->
                                "$path ($type)"
                            } +
                            ". Gson cannot deserialize polymorphic sealed interfaces. " +
                            "Use SerializationBackend.KOTLINX for this operation.",
                    )
                }
            }
            if (allViolations.isNotEmpty()) {
                throw GradleException(
                    "GSON response generation is not supported for operations with interface/union types:\n" +
                        allViolations.joinToString("\n"),
                )
            }
        }

        val fileSpecs = mutableListOf<FileSpec>()

        // Generate enum classes needed by response types (even if generateVariables is off)
        fileSpecs.addAll(generateEnumArtifacts(schemaIndex, pkg, "responses", backend))

        parsedOperations.forEach { (operation, selectionSet) ->
            fileSpecs.addAll(modelGenerator.generate(operation, selectionSet, pkg))
            logger.info("Generated ${operation.name}Data response model")
        }

        return fileSpecs
    }

    private fun writeFile(
        fileSpec: FileSpec,
        outputDirectory: File,
    ) {
        fileSpec.writeTo(outputDirectory)
    }

    /**
     * Generates Kotlin enum classes from the schema index.
     */
    private fun generateEnumArtifacts(
        schemaIndex: SchemaIndex,
        pkg: String,
        logPrefix: String,
        backend: SerializationBackend,
    ): List<FileSpec> {
        val enums = schemaIndex.enums
        return if (enums.isNotEmpty()) {
            val fileSpecs = EnumGenerator.generate(enums, pkg, backend)
            logger.info("[$logPrefix] Generated ${enums.size} enum class(es)")
            fileSpecs
        } else {
            emptyList()
        }
    }

    /**
     * Converts a codegen [OperationType] to a graphql-java [OperationDefinition.Operation].
     */
    private fun OperationType.toGraphQLOperation(): OperationDefinition.Operation = when (this) {
        OperationType.QUERY -> OperationDefinition.Operation.QUERY
        OperationType.MUTATION -> OperationDefinition.Operation.MUTATION
        OperationType.SUBSCRIPTION -> OperationDefinition.Operation.SUBSCRIPTION
    }

    /**
     * Recursively finds abstract type (interface/union) references within a
     * [ResponseSelectionSet] and returns the type name and response path for
     * each violation.
     *
     * @param selectionSet The response selection set to inspect.
     * @param schemaIndex The schema index for type lookup.
     * @param currentPath The current response path being traversed.
     * @return A list of pairs where each pair is (typeName, path).
     */
    private fun findAbstractTypePaths(
        selectionSet: ResponseSelectionSet,
        schemaIndex: SchemaIndex,
        currentPath: List<String> = emptyList(),
    ): List<Pair<String, String>> {
        val violations = mutableListOf<Pair<String, String>>()
        for (field in selectionSet.fields) {
            val childSet = field.selectionSet ?: continue
            val typeName = resolveNamedType(field.outputType)
            val fieldPath = currentPath + field.responseName
            if (schemaIndex.isAbstractType(typeName)) {
                violations.add(typeName to fieldPath.joinToString("."))
            } else {
                violations.addAll(findAbstractTypePaths(childSet, schemaIndex, fieldPath))
            }
        }
        return violations
    }

    /**
     * Extracts the named type from a [GraphQLType], unwrapping list wrappers.
     */
    private fun resolveNamedType(type: GraphQLType): String = when (type) {
        is GraphQLType.Named -> type.name
        is GraphQLType.List -> resolveNamedType(type.of)
    }
}
