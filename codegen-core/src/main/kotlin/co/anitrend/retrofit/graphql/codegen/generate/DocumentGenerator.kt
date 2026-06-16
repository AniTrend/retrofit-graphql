package co.anitrend.retrofit.graphql.codegen.generate

import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * Generates a `GraphQLDocuments` Kotlin object containing the full document text
 * for each operation as raw string constants.
 *
 * Example output:
 * ```kotlin
 * public object GraphQLDocuments {
 *     public const val GetCurrentUser: String = """
 *         query GetCurrentUser {
 *           Viewer {
 *             id
 *             name
 *           }
 *         }
 *     """
 * }
 * ```
 */
object DocumentGenerator {

    fun generate(
        operations: List<GraphQLOperationInfo>,
        packageName: String,
    ): FileSpec {
        return FileSpec.builder(packageName, "GraphQLDocuments")
            .addType(
                TypeSpec.objectBuilder("GraphQLDocuments")
                    .addModifiers(KModifier.PUBLIC)
                    .apply {
                        operations.forEach { op ->
                            addProperty(
                                PropertySpec.builder(op.name, String::class)
                                    .addModifiers(KModifier.PUBLIC, KModifier.CONST)
                                    .initializer("%S", op.document)
                                    .build()
                            )
                        }
                    }
                    .build()
            )
            .build()
    }
}
