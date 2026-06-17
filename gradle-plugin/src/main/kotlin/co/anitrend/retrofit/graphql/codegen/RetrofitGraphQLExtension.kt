package co.anitrend.retrofit.graphql.codegen

import org.gradle.api.Action
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

//
// DSL extension for configuring the retrofit-graphql code generation plugin.
//
// Example usage in a consumer's build.gradle.kts:
// ```kotlin
// retrofitGraphQL {
//     packageName.set("co.anitrend.graphql.generated")
//     schema.set(file("src/main/graphql/schema.graphql"))
//     operations.from(fileTree("src/main/graphql") {
//         include("**\/*.graphql")
//     })
//     generateOperationConstants.set(true)
//     generateDocuments.set(true)
//     generateHashes.set(true)
//     generateVariables.set(true)
//     scalars {
//         map("DateTime", "kotlin.String")
//         map("Upload", "okhttp3.MultipartBody.Part")
//     }
// }
// ```
//
abstract class RetrofitGraphQLExtension {

    //
    // The Kotlin package name for generated source files.
    // Defaults to "co.anitrend.graphql.generated".
    //
    abstract val packageName: Property<String>

    //
    // The source directory or file tree containing *.graphql operation files.
    //
    abstract val operations: ConfigurableFileTree

    //
    // The schema.graphql file containing type definitions (input objects, enums, scalars).
    // Optional; if not set, variable and input object generation is skipped.
    //
    abstract val schema: RegularFileProperty

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
    // operation request helpers. Requires [schema] to be set for input objects.
    // Default false.
    //
    abstract val generateVariables: Property<Boolean>

    //
    // The output directory for generated sources.
    // Defaults to "${buildDir}/generated/source/graphql".
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
        generateOperationConstants.convention(true)
        generateDocuments.convention(true)
        generateHashes.convention(true)
        generateVariables.convention(false)
    }
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
