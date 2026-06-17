package co.anitrend.retrofit.graphql.codegen

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

//
// DSL extension for a single GraphQL code-generation target.
//
// Example usage in a consumer's build.gradle.kts:
// ```kotlin
// retrofitGraphQL {
//     target("anilist") {
//         packageName.set("co.anitrend.graphql.generated.anilist")
//         schema.set(file("src/main/graphql/anilist/schema.graphql"))
//         operations.from(fileTree("src/main/graphql/anilist") {
//             include("**\/*.graphql")
//         })
//     }
// }
// ```
//
abstract class GraphQLTargetExtension {

    //
    // The Kotlin package name for generated source files.
    // Defaults to "co.anitrend.graphql.generated".
    //
    abstract val packageName: Property<String>

    //
    // The source directory or file tree containing *.graphql operation files.
    //
    abstract val operations: ConfigurableFileCollection

    //
    // The schema.graphql file containing type definitions (input objects, enums, scalars).
    // Optional; if not set, variable and input object generation is skipped.
    //
    abstract val schema: RegularFileProperty

    //
    // Whether to generate GraphQLOperations constants.
    // Falls back to the common {} block if not set on this target.
    //
    abstract val generateOperationConstants: Property<Boolean>

    //
    // Whether to generate GraphQLDocuments constants.
    // Falls back to the common {} block if not set on this target.
    //
    abstract val generateDocuments: Property<Boolean>

    //
    // Whether to generate GraphQLHashes constants.
    // Falls back to the common {} block if not set on this target.
    //
    abstract val generateHashes: Property<Boolean>

    //
    // Whether to generate variable classes, input objects, enum constants, and
    // operation request helpers. Requires [schema] to be set for input objects.
    // Falls back to the common {} block if not set on this target.
    //
    abstract val generateVariables: Property<Boolean>

    //
    // The output directory for generated sources.
    // Defaults to "${buildDir}/generated/source/graphql/${targetName}".
    //
    abstract val outputDir: DirectoryProperty

    //
    // Scalar type mappings. Configure custom GraphQL scalar types to Kotlin types.
    //
    internal val scalarMappings = ScalarMappingExtension()

    //
    // Configure scalar type mappings.
    //
    // ```kotlin
    // scalars {
    //     map("DateTime", "kotlin.String")
    // }
    // ```
    //
    fun scalars(action: Action<ScalarMappingExtension>) {
        action.execute(scalarMappings)
    }

    init {
        packageName.convention("co.anitrend.graphql.generated")
    }
}

//
// Common settings shared across all GraphQL code-generation targets.
// Each target can override these individually.
//
abstract class CommonExtension {

    //
    // Whether to generate GraphQLOperations constants. Default true.
    //
    abstract val generateOperationConstants: Property<Boolean>

    //
    // Whether to generate GraphQLDocuments constants. Default true.
    //
    abstract val generateDocuments: Property<Boolean>

    //
    // Whether to generate GraphQLHashes constants. Default true.
    //
    abstract val generateHashes: Property<Boolean>

    //
    // Whether to generate variable classes, input objects, enum constants, and
    // operation request helpers. Default false.
    //
    abstract val generateVariables: Property<Boolean>

    init {
        generateOperationConstants.convention(true)
        generateDocuments.convention(true)
        generateHashes.convention(true)
        generateVariables.convention(false)
    }
}

//
// DSL extension for configuring the retrofit-graphql code generation plugin.
//
// Supports two modes:
//
// 1. Legacy single-target mode (backward compatible):
// ```kotlin
// retrofitGraphQL {
//     packageName.set("co.anitrend.graphql.generated")
//     schema.set(file("src/main/graphql/schema.graphql"))
//     operations.from(fileTree("src/main/graphql") {
//         include("**\/*.graphql")
//     })
// }
// ```
//
// 2. Multi-target mode:
// ```kotlin
// retrofitGraphQL {
//     common {
//         generateDocuments.set(true)
//         generateOperationConstants.set(true)
//         generateHashes.set(true)
//         generateVariables.set(false)
//     }
//
//     target("anilist") {
//         packageName.set("co.anitrend.graphql.generated.anilist")
//         schema.set(file("src/main/graphql/anilist/schema.graphql"))
//         operations.from(fileTree("src/main/graphql/anilist") {
//             include("**\/*.graphql")
//         })
//     }
// }
// ```
//
open class RetrofitGraphQLExtension {

    //
    // Common settings shared across all targets.
    // Set by the plugin during apply.
    //
    lateinit var common: CommonExtension
        internal set

    //
    // Named container of code-generation targets.
    // Set by the plugin during apply.
    //
    lateinit var targets: NamedDomainObjectContainer<GraphQLTargetExtension>
        internal set

    //
    // The default "main" target used for legacy single-target configuration.
    // Lazily created when legacy properties are accessed.
    //
    private var mainTargetInternal: GraphQLTargetExtension? = null

    private fun mainTarget(): GraphQLTargetExtension {
        if (mainTargetInternal == null) {
            mainTargetInternal = targets.maybeCreate("main")
        }
        return mainTargetInternal!!
    }

    //
    // Register a named code-generation target.
    //
    fun target(name: String, action: Action<GraphQLTargetExtension>) {
        action.execute(targets.maybeCreate(name))
    }

    // -------------------------------------------------------------------------
    // Legacy sugar -- delegates to the default "main" target
    // -------------------------------------------------------------------------

    //
    // The Kotlin package name for generated source files.
    // Delegates to the "main" target.
    //
    val packageName: Property<String> get() = mainTarget().packageName

    //
    // The source directory or file tree containing *.graphql operation files.
    // Delegates to the "main" target.
    //
    val operations: ConfigurableFileCollection get() = mainTarget().operations

    //
    // The schema.graphql file containing type definitions.
    // Delegates to the "main" target.
    //
    val schema: RegularFileProperty get() = mainTarget().schema

    //
    // Whether to generate GraphQLOperations constants.
    // Delegates to the "main" target.
    //
    val generateOperationConstants: Property<Boolean> get() = mainTarget().generateOperationConstants

    //
    // Whether to generate GraphQLDocuments constants.
    // Delegates to the "main" target.
    //
    val generateDocuments: Property<Boolean> get() = mainTarget().generateDocuments

    //
    // Whether to generate GraphQLHashes constants.
    // Delegates to the "main" target.
    //
    val generateHashes: Property<Boolean> get() = mainTarget().generateHashes

    //
    // Whether to generate variable classes, input objects, enum constants, and
    // operation request helpers.
    // Delegates to the "main" target.
    //
    val generateVariables: Property<Boolean> get() = mainTarget().generateVariables

    //
    // The output directory for generated sources.
    // Delegates to the "main" target.
    //
    val outputDir: DirectoryProperty get() = mainTarget().outputDir

    //
    // Configure scalar type mappings on the default "main" target.
    //
    fun scalars(action: Action<ScalarMappingExtension>) {
        action.execute(mainTarget().scalarMappings)
    }

    //
    // Access to scalar mappings on the default "main" target.
    //
    internal val scalarMappings: ScalarMappingExtension get() = mainTarget().scalarMappings
}

//
// Accumulates GraphQL-scalar-to-Kotlin-type mappings.
//
open class ScalarMappingExtension {
    private val mappings = linkedMapOf<String, String>()

    //
    // Map a GraphQL scalar type name to a Kotlin type.
    //
    // @param graphqlType The GraphQL scalar name (e.g. "DateTime").
    // @param kotlinType The fully-qualified Kotlin type (e.g. "kotlin.String").
    //
    fun map(graphqlType: String, kotlinType: String) {
        mappings[graphqlType] = kotlinType
    }

    internal fun toMap(): Map<String, String> = mappings.toMap()
}
