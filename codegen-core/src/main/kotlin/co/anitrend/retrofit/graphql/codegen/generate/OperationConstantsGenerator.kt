package co.anitrend.retrofit.graphql.codegen.generate

import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import co.anitrend.retrofit.graphql.codegen.model.OperationType
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * Generates a `GraphQLOperations` Kotlin object containing operation name constants,
 * grouped by operation type (Query, Mutation, Subscription).
 *
 * Example output:
 * ```kotlin
 * public object GraphQLOperations {
 *     public object Query {
 *         public const val GetCurrentUser: String = "GetCurrentUser"
 *         public const val GetMarketPlaceApps: String = "GetMarketPlaceApps"
 *     }
 *     public object Mutation
 *     public object Subscription
 * }
 * ```
 */
object OperationConstantsGenerator {

    fun generate(
        operations: List<GraphQLOperationInfo>,
        packageName: String,
    ): FileSpec {
        val grouped = operations.groupBy { it.type }

        return FileSpec.builder(packageName, "GraphQLOperations")
            .addType(
                TypeSpec.objectBuilder("GraphQLOperations")
                    .addModifiers(KModifier.PUBLIC)
                    .addType(buildOperationGroup(OperationType.QUERY, grouped))
                    .addType(buildOperationGroup(OperationType.MUTATION, grouped))
                    .addType(buildOperationGroup(OperationType.SUBSCRIPTION, grouped))
                    .build()
            )
            .build()
    }

    private fun buildOperationGroup(
        type: OperationType,
        grouped: Map<OperationType, List<GraphQLOperationInfo>>,
    ): TypeSpec {
        val operations = grouped[type].orEmpty()
        val typeName = type.name.lowercase().replaceFirstChar { it.uppercase() }

        return TypeSpec.objectBuilder(typeName)
            .addModifiers(KModifier.PUBLIC)
            .apply {
                operations.forEach { op ->
                    addProperty(
                        PropertySpec.builder(op.name, String::class)
                            .addModifiers(KModifier.PUBLIC, KModifier.CONST)
                            .initializer("%S", op.name)
                            .build()
                    )
                }
            }
            .build()
    }
}
