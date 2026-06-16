package co.anitrend.retrofit.graphql.codegen

import co.anitrend.retrofit.graphql.codegen.generate.DocumentGenerator
import co.anitrend.retrofit.graphql.codegen.generate.HashConstantsGenerator
import co.anitrend.retrofit.graphql.codegen.generate.OperationConstantsGenerator
import co.anitrend.retrofit.graphql.codegen.generate.RegistryGenerator
import co.anitrend.retrofit.graphql.codegen.model.GraphQLFragmentInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import co.anitrend.retrofit.graphql.codegen.parser.GraphQLDocumentParser
import co.anitrend.retrofit.graphql.codegen.resolve.FragmentResolver
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Gradle task that parses .graphql operation files and generates Kotlin source code:
 * - GraphQLOperations (operation name constants)
 * - GraphQLDocuments (document text constants)
 * - GraphQLHashes (APQ hash constants)
 * - GeneratedGraphQLRegistry (registry implementing GraphQLDocumentRegistry)
 */
abstract class GenerateGraphQLSourcesTask : DefaultTask() {

    @get:Input
    abstract val packageName: Property<String>

    @get:InputFiles
    abstract val operationsDir: ConfigurableFileTree

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val generateOperationConstants: Property<Boolean>

    @get:Input
    abstract val generateDocuments: Property<Boolean>

    @get:Input
    abstract val generateHashes: Property<Boolean>

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

        logger.lifecycle(
            "Generated GraphQL sources for ${resolvedOperations.size} operation(s) " +
                "in package '$pkg'"
        )
    }

    private fun writeFile(fileSpec: com.squareup.kotlinpoet.FileSpec, outputDirectory: File) {
        fileSpec.writeTo(outputDirectory)
    }
}
