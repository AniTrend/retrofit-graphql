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
import co.anitrend.retrofit.graphql.codegen.model.SchemaType
import co.anitrend.retrofit.graphql.codegen.parser.GraphQLDocumentParser
import co.anitrend.retrofit.graphql.codegen.parser.SchemaParser
import co.anitrend.retrofit.graphql.codegen.resolve.FragmentResolver
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
 * - Enum constants from schema
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
                            "found in ${file.name}. Fragment names must be unique."
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
                    "Operation names must be unique across all .graphql files."
            )
        }

        // Flatten fragments into operations
        val resolvedOperations = resolver.resolve(allOperations, allFragments)

        // Parse schema if available
        val schemaTypes: List<SchemaType>
        val scalarMap: Map<String, String>
        val schemaTypeNames: Set<String>
        if (schemaFile.isPresent && schemaFile.get().asFile.exists()) {
            val schemaParser = SchemaParser()
            schemaTypes = schemaParser.parse(schemaFile.get().asFile)
            scalarMap = scalarMappings.orNull ?: emptyMap()
            schemaTypeNames = schemaTypes.map { it.let { t ->
                when (t) {
                    is SchemaType.InputObject -> t.name
                    is SchemaType.Enum -> t.name
                    is SchemaType.Scalar -> t.name
                }
            } }.toSet()
        } else {
            if (generateVariables.get()) {
                logger.warn(
                    "generateVariables is enabled but no schema file is set. " +
                        "Input object and enum generation will be skipped. " +
                        "Variable classes will use scalar mappings only."
                )
            }
            schemaTypes = emptyList()
            scalarMap = scalarMappings.orNull ?: emptyMap()
            schemaTypeNames = emptySet()
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
                resolvedOperations, schemaTypes, scalarMap, schemaTypeNames, pkg, outputDirectory
            )
        }

        logger.lifecycle(
            "Generated GraphQL sources for ${resolvedOperations.size} operation(s) " +
                "in package '$pkg'"
        )
    }

    private fun generateVariableArtifacts(
        operations: List<GraphQLOperationInfo>,
        schemaTypes: List<SchemaType>,
        scalarMap: Map<String, String>,
        schemaTypeNames: Set<String>,
        pkg: String,
        outputDirectory: File,
    ) {
        // Validate unknown scalars in variables
        validateScalars(operations, scalarMap, schemaTypeNames)

        // Generate per-operation variable classes
        operations.forEach { operation ->
            val varFile = VariableClassGenerator.generate(
                operation, pkg, scalarMap, schemaTypeNames
            )
            if (varFile != null) {
                writeFile(varFile, outputDirectory)
                logger.info("Generated ${operation.name}Variables")
            }
        }

        // Generate input object classes from schema
        val inputObjects = schemaTypes.filterIsInstance<SchemaType.InputObject>()
        if (inputObjects.isNotEmpty()) {
            InputObjectGenerator.generate(inputObjects, pkg, scalarMap, schemaTypeNames)
                .forEach { writeFile(it, outputDirectory) }
            logger.info("Generated ${inputObjects.size} input object class(es)")
        }

        // Generate enum constants from schema
        val enums = schemaTypes.filterIsInstance<SchemaType.Enum>()
        if (enums.isNotEmpty()) {
            val enumFile = EnumGenerator.generateAsStringConstants(enums, pkg)
            if (enumFile != null) {
                writeFile(enumFile, outputDirectory)
                logger.info("Generated enum constants for ${enums.size} enum type(s)")
            }
        }

        // Generate per-operation request helpers
        operations.forEach { operation ->
            val reqFile = OperationRequestGenerator.generate(
                operation, pkg, scalarMap, schemaTypeNames
            )
            writeFile(reqFile, outputDirectory)
            logger.info("Generated ${operation.name} request helper")
        }
    }

    /**
     * Validates that all variable types reference known scalars or schema types.
     * Throws if an unknown scalar is found without a mapping.
     */
    private fun validateScalars(
        operations: List<GraphQLOperationInfo>,
        scalarMap: Map<String, String>,
        schemaTypeNames: Set<String>,
    ) {
        val builtInScalars = setOf("String", "Int", "Float", "Boolean", "ID")

        for (operation in operations) {
            for (variable in operation.variables) {
                val typeNames = collectTypeNames(variable.type)
                for (typeName in typeNames) {
                    if (typeName in builtInScalars) continue
                    if (typeName in schemaTypeNames) continue
                    if (typeName in scalarMap) continue
                    throw IllegalArgumentException(
                        "Unknown type '$typeName' in variable '${variable.name}' " +
                            "of operation '${operation.name}'. " +
                            "Add a scalar mapping in the retrofitGraphQL {} extension, e.g.:\n" +
                            "  scalars { map(\"$typeName\", \"kotlin.String\") }"
                    )
                }
            }
        }
    }

    private fun collectTypeNames(type: co.anitrend.retrofit.graphql.codegen.model.GraphQLType): Set<String> {
        return when (type) {
            is co.anitrend.retrofit.graphql.codegen.model.GraphQLType.Named -> setOf(type.name)
            is co.anitrend.retrofit.graphql.codegen.model.GraphQLType.List -> collectTypeNames(type.of)
        }
    }

    private fun writeFile(fileSpec: com.squareup.kotlinpoet.FileSpec, outputDirectory: File) {
        fileSpec.writeTo(outputDirectory)
    }
}
