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

package co.anitrend.retrofit.graphql.codegen.generate

import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
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
    private val REGISTRY_INTERFACE =
        ClassName(
            "co.anitrend.retrofit.graphql.model",
            "GraphQLDocumentRegistry",
        )

    fun generate(
        operations: List<GraphQLOperationInfo>,
        packageName: String,
    ): FileSpec {
        // Use the actual package name so KotlinPoet recognizes same-package
        // references and avoids emitting bare imports (which Kotlin 2.4+ rejects).
        val operationsClass = ClassName(packageName, "GraphQLOperations")
        val documentsClass = ClassName(packageName, "GraphQLDocuments")
        val hashesClass = ClassName(packageName, "GraphQLHashes")
        return FileSpec.builder(packageName, "GeneratedGraphQLRegistry")
            .addType(
                TypeSpec.objectBuilder("GeneratedGraphQLRegistry")
                    .addModifiers(KModifier.PUBLIC)
                    .addSuperinterface(REGISTRY_INTERFACE)
                    .addFunction(buildDocumentFunction(operations, operationsClass, documentsClass))
                    .addFunction(buildHashFunction(operations, operationsClass, hashesClass))
                    .build(),
            )
            .build()
    }

    private fun buildDocumentFunction(
        operations: List<GraphQLOperationInfo>,
        operationsClass: ClassName,
        documentsClass: ClassName,
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
                        operationsClass,
                        documentsClass,
                    )
                }
            }
            .addStatement("else -> null")
            .endControlFlow()
            .build()
    }

    private fun buildHashFunction(
        operations: List<GraphQLOperationInfo>,
        operationsClass: ClassName,
        hashesClass: ClassName,
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
                        operationsClass,
                        hashesClass,
                    )
                }
            }
            .addStatement("else -> null")
            .endControlFlow()
            .build()
    }
}
