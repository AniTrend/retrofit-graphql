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

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

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
abstract class GraphQLTargetExtension @Inject constructor(val name: String) {
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
    // Whether to generate variable classes, input objects, enum classes, and
    // operation request helpers. Requires [schema] to be set for input objects.
    // Falls back to the common {} block if not set on this target.
    //
    abstract val generateVariables: Property<Boolean>

    //
    // Whether to generate response model data classes from operation
    // selection sets. Default false. Requires [schema] to be set.
    // Falls back to the common {} block if not set on this target.
    //
    abstract val generateResponses: Property<Boolean>

    //
    // Which serialization backend to use for generated annotations.
    //
    // Accepted values (case-sensitive):
    // - `"NONE"` — No serialization annotations emitted. Classes are generated
    //   as plain data holders. This is the default.
    // - `"KOTLINX"` — Emits `@Serializable`, `@SerialName`, and polymorphic
    //   markers. Required when [generateResponses] is true.
    // - `"GSON"` — Emits `@SerializedName` on properties. Not compatible
    //   with [generateResponses] (Gson cannot deserialize polymorphic sealed
    //   interfaces needed for GraphQL unions/interfaces).
    //
    // Auto-selection: When set to `"NONE"` (default) and [generateResponses]
    // is `true`, the codegen automatically selects `"KOTLINX"`. This is a
    // convenience for consumers who only want typed responses.
    //
    // Falls back to the common {} block if not set on this target.
    // Default: "NONE"
    //
    abstract val serializationBackend: Property<String>

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
    // Whether to generate variable classes, input objects, enum classes, and
    // operation request helpers. Default false.
    //
    abstract val generateVariables: Property<Boolean>

    //
    // Whether to generate response model data classes from operation
    // selection sets. Default false. Requires [schema] to be set.
    //
    abstract val generateResponses: Property<Boolean>

    //
    // Which serialization backend to use for generated annotations across
    // all targets. Each target can override this individually via its own
    // [GraphQLTargetExtension.serializationBackend].
    //
    // Accepted values: `"NONE"`, `"KOTLINX"`, `"GSON"` (case-sensitive).
    // Default: `"NONE"`. When `"NONE"` and [generateResponses] is `true`,
    // the codegen auto-selects `"KOTLINX"`.
    //
    // @see GraphQLTargetExtension.serializationBackend
    //
    abstract val serializationBackend: Property<String>

    init {
        generateOperationConstants.convention(true)
        generateDocuments.convention(true)
        generateHashes.convention(true)
        generateVariables.convention(false)
        generateResponses.convention(false)
        serializationBackend.convention("NONE")
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
open class RetrofitGraphQLExtension @Inject constructor(objects: ObjectFactory) {
    //
    // Common settings shared across all targets.
    // Set by the plugin during apply (overrides constructor default).
    //
    var common: CommonExtension = objects.newInstance(CommonExtension::class.java)
        internal set

    //
    // Named container of code-generation targets.
    // Set by the plugin during apply. Left null before that to avoid
    // triggering domain-object construction during Gradle's decoration phase.
    //
    var targets: NamedDomainObjectContainer<GraphQLTargetExtension>? = null
        internal set

    // -------------------------------------------------------------------------
    // Legacy properties — stored directly on the extension so Gradle's
    // $gradleInitAttach does not walk into the targets container prematurely.
    // When the plugin creates a default "main" target, it copies values from
    // these properties into the target.
    // -------------------------------------------------------------------------

    @get:org.gradle.api.tasks.Input
    val packageName: Property<String> = objects.property(String::class.java)

    @get:org.gradle.api.tasks.InputFiles
    val operations: ConfigurableFileCollection = objects.fileCollection()

    @get:org.gradle.api.tasks.InputFile
    @get:org.gradle.api.tasks.Optional
    val schema: RegularFileProperty = objects.fileProperty()

    @get:org.gradle.api.tasks.Input
    val generateOperationConstants: Property<Boolean> = objects.property(Boolean::class.java)

    @get:org.gradle.api.tasks.Input
    val generateDocuments: Property<Boolean> = objects.property(Boolean::class.java)

    @get:org.gradle.api.tasks.Input
    val generateHashes: Property<Boolean> = objects.property(Boolean::class.java)

    @get:org.gradle.api.tasks.Input
    val generateVariables: Property<Boolean> = objects.property(Boolean::class.java)

    @get:org.gradle.api.tasks.Input
    val generateResponses: Property<Boolean> = objects.property(Boolean::class.java)

    @get:org.gradle.api.tasks.Input
    val serializationBackend: Property<String> = objects.property(String::class.java)

    @get:org.gradle.api.tasks.OutputDirectory
    val outputDir: DirectoryProperty = objects.directoryProperty()

    //
    // The default "main" target — lazily created inside the targets container
    // when a legacy property is accessed, or when the plugin needs it.
    //
    private var mainTargetInternal: GraphQLTargetExtension? = null

    fun mainTarget(): GraphQLTargetExtension {
        if (mainTargetInternal == null) {
            mainTargetInternal = targets?.maybeCreate("main")
                ?: throw IllegalStateException(
                    "retrofitGraphQL extension has not been fully initialized. " +
                        "This is a bug in the plugin.",
                )
        }
        return mainTargetInternal!!
    }

    //
    // Register a named code-generation target.
    //
    fun target(
        name: String,
        action: Action<GraphQLTargetExtension>,
    ) {
        action.execute(
            targets?.maybeCreate(name)
                ?: throw IllegalStateException(
                    "retrofitGraphQL extension has not been fully initialized.",
                ),
        )
    }

    //
    // DSL function for configuring common settings via 'common { ... }' block.
    //
    fun common(action: Action<CommonExtension>) {
        action.execute(common)
    }

    // -------------------------------------------------------------------------
    // Scalar mappings support
    // -------------------------------------------------------------------------

    internal val scalarMappings = ScalarMappingExtension()

    fun scalars(action: Action<ScalarMappingExtension>) {
        action.execute(scalarMappings)
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
    fun map(
        graphqlType: String,
        kotlinType: String,
    ) {
        mappings[graphqlType] = kotlinType
    }

    internal fun toMap(): Map<String, String> = mappings.toMap()
}
