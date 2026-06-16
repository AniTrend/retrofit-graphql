package co.anitrend.retrofit.graphql.codegen

import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

/**
 * DSL extension for configuring the retrofit-graphql code generation plugin.
 *
 * Example usage in a consumer's build.gradle.kts:
 * ```kotlin
 * retrofitGraphQL {
 *     packageName.set("co.anitrend.graphql.generated")
 *     operations.from(fileTree("src/main/graphql") {
 *         include("**\/*.graphql")
 *     })
 *     generateOperationConstants.set(true)
 *     generateDocuments.set(true)
 *     generateHashes.set(true)
 * }
 * ```
 */
abstract class RetrofitGraphQLExtension {

    /**
     * The Kotlin package name for generated source files.
     * Defaults to "co.anitrend.graphql.generated".
     */
    abstract val packageName: Property<String>

    /**
     * The source directory or file tree containing *.graphql operation files.
     */
    abstract val operations: ConfigurableFileTree

    /**
     * Whether to generate GraphQLOperations constants. Default true.
     */
    abstract val generateOperationConstants: Property<Boolean>

    /**
     * Whether to generate GraphQLDocuments constants. Default true.
     */
    abstract val generateDocuments: Property<Boolean>

    /**
     * Whether to generate GraphQLHashes constants. Default true.
     */
    abstract val generateHashes: Property<Boolean>

    /**
     * The output directory for generated sources.
     * Defaults to "${buildDir}/generated/source/graphql".
     */
    abstract val outputDir: DirectoryProperty

    init {
        packageName.convention("co.anitrend.graphql.generated")
        generateOperationConstants.convention(true)
        generateDocuments.convention(true)
        generateHashes.convention(true)
    }
}
