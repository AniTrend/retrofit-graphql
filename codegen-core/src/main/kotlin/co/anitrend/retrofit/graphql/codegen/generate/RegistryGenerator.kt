package co.anitrend.retrofit.graphql.codegen.generate

import co.anitrend.retrofit.graphql.codegen.hash.APQHashGenerator
import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec

/**
 * Generates a `GeneratedGraphQLRegistry` Kotlin object that implements
 * `GraphQLDocumentRegistry`. Maps operation names to their documents and hashes.
 *
 * Example output:
 * ```kotlin
 * public object GeneratedGraphQLRegistry : GraphQLDocumentRegistry {
 *     override fun document(operationName: String): String? =
 *         when (operationName) {
 *             GraphQLOperations.Query.GetCurrentUser -> GraphQLDocuments.GetCurrentUser
 *             GraphQLOperations.Query.GetMarketPlaceApps -> GraphQLDocuments.GetMarketPlaceApps
 *             else -> null
 *         }
 *
 *     override fun hash(operationName: String): String? =
 *         when (operationName) {
 *             GraphQLOperations.Query.GetCurrentUser -> GraphQLHashes.GetCurrentUser
 *             GraphQLOperations.Query.GetMarketPlaceApps -> GraphQLHashes.GetMarketPlaceApps
 *             else -> null
 *         }
 * }
 * ```
 */
object RegistryGenerator {

    private val REGISTRY_INTERFACE = ClassName(
        "co.anitrend.retrofit.graphql.model",
        "GraphQLDocumentRegistry"
    )
    private val OPERATIONS_CLASS = ClassName("", "GraphQLOperations")
    private val DOCUMENTS_CLASS = ClassName("", "GraphQLDocuments")
    private val HASHES_CLASS = ClassName("", "GraphQLHashes")

    fun generate(
        operations: List<GraphQLOperationInfo>,
        packageName: String,
    ): FileSpec {
        return FileSpec.builder(packageName, "GeneratedGraphQLRegistry")
            .addType(
                TypeSpec.objectBuilder("GeneratedGraphQLRegistry")
                    .addModifiers(KModifier.PUBLIC)
                    .addSuperinterface(REGISTRY_INTERFACE)
                    .addFunction(buildDocumentFunction(operations))
                    .addFunction(buildHashFunction(operations))
                    .build()
            )
            .build()
    }

    private fun buildDocumentFunction(
        operations: List<GraphQLOperationInfo>,
    ): FunSpec {
        return FunSpec.builder("document")
            .addModifiers(KModifier.OVERRIDE, KModifier.PUBLIC)
            .addParameter("operationName", String::class)
            .returns(ClassName("kotlin", "String").copy(nullable = true))
            .beginControlFlow("return when (operationName)")
            .apply {
                operations.forEach { op ->
                    val constRef = "${op.type.name.lowercase().replaceFirstChar { it.uppercase() }}.${op.name}"
                    addStatement(
                        "%T.$constRef -> %T.${op.name}",
                        OPERATIONS_CLASS, DOCUMENTS_CLASS
                    )
                }
            }
            .addStatement("else -> null")
            .endControlFlow()
            .build()
    }

    private fun buildHashFunction(
        operations: List<GraphQLOperationInfo>,
    ): FunSpec {
        return FunSpec.builder("hash")
            .addModifiers(KModifier.OVERRIDE, KModifier.PUBLIC)
            .addParameter("operationName", String::class)
            .returns(ClassName("kotlin", "String").copy(nullable = true))
            .beginControlFlow("return when (operationName)")
            .apply {
                operations.forEach { op ->
                    val constRef = "${op.type.name.lowercase().replaceFirstChar { it.uppercase() }}.${op.name}"
                    addStatement(
                        "%T.$constRef -> %T.${op.name}",
                        OPERATIONS_CLASS, HASHES_CLASS
                    )
                }
            }
            .addStatement("else -> null")
            .endControlFlow()
            .build()
    }
}
