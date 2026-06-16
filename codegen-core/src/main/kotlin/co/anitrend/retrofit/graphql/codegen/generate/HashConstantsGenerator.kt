package co.anitrend.retrofit.graphql.codegen.generate

import co.anitrend.retrofit.graphql.codegen.hash.APQHashGenerator
import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * Generates a `GraphQLHashes` Kotlin object containing APQ SHA-256 hashes
 * for each operation's flattened document.
 *
 * Example output:
 * ```kotlin
 * public object GraphQLHashes {
 *     public const val GetCurrentUser: String = "a1b2c3d4e5f6..."
 *     public const val GetMarketPlaceApps: String = "f6e5d4c3b2a1..."
 * }
 * ```
 */
object HashConstantsGenerator {

    fun generate(
        operations: List<GraphQLOperationInfo>,
        packageName: String,
    ): FileSpec {
        return FileSpec.builder(packageName, "GraphQLHashes")
            .addType(
                TypeSpec.objectBuilder("GraphQLHashes")
                    .addModifiers(KModifier.PUBLIC)
                    .apply {
                        operations.forEach { op ->
                            val hash = APQHashGenerator.hash(op.document)
                            addProperty(
                                PropertySpec.builder(op.name, String::class)
                                    .addModifiers(KModifier.PUBLIC, KModifier.CONST)
                                    .initializer("%S", hash)
                                    .build()
                            )
                        }
                    }
                    .build()
            )
            .build()
    }
}
