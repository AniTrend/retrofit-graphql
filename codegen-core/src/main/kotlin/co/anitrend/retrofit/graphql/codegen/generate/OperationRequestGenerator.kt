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

import co.anitrend.retrofit.graphql.codegen.mapping.GraphQLTypeMapper
import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec

/**
 * Generates per-operation helper objects.
 *
 * For operations with variables, generates:
 * ```kotlin
 * public object GetMarketPlaceApps : GraphQLOperation<GetMarketPlaceAppsVariables> {
 *     override val name: String = GraphQLOperations.Query.GetMarketPlaceApps
 *     override val document: String = GraphQLDocuments.GetMarketPlaceApps
 *     override val sha256Hash: String = GraphQLHashes.GetMarketPlaceApps
 *
 *     public fun request(after: String? = null, before: String? = null, first: Int): GraphQLOperationRequest<GetMarketPlaceAppsVariables> =
 *         GraphQLOperationRequest(query = document, operationName = name, variables = GetMarketPlaceAppsVariables(...))
 * }
 * ```
 *
 * For operations without variables:
 * ```kotlin
 * public object GetCurrentUser : GraphQLNoVarOperation {
 *     override val name: String = ...
 *     override val document: String = ...
 *     override val sha256Hash: String = ...
 * }
 * ```
 *
 * Request helpers use the backend-neutral
 * [co.anitrend.retrofit.graphql.model.request.GraphQLOperationRequest] contract,
 * not the legacy serializer-coupled
 * [co.anitrend.retrofit.graphql.model.GraphQLRequest]. Operation name,
 * document, hash, and registry semantics are unchanged.
 */
object OperationRequestGenerator {
    private val GRAPHQL_OPERATION = ClassName("co.anitrend.retrofit.graphql.model", "GraphQLOperation")
    private val GRAPHQL_NO_VAR_OPERATION = ClassName("co.anitrend.retrofit.graphql.model", "GraphQLNoVarOperation")
    private val GRAPHQL_OPERATION_REQUEST = ClassName("co.anitrend.retrofit.graphql.model.request", "GraphQLOperationRequest")

    /**
     * Generates a request helper object for a single operation.
     */
    fun generate(
        operation: GraphQLOperationInfo,
        packageName: String,
        scalarMappings: Map<String, String>,
        schemaIndex: SchemaIndex,
    ): FileSpec {
        val operationsClass = ClassName(packageName, "GraphQLOperations")
        val documentsClass = ClassName(packageName, "GraphQLDocuments")
        val hashesClass = ClassName(packageName, "GraphQLHashes")
        val hasVariables = operation.variables.isNotEmpty()
        val variablesClassName = "${operation.name}Variables"
        val variablesClass = ClassName(packageName, variablesClassName)

        val superInterface =
            if (hasVariables) {
                GRAPHQL_OPERATION.parameterizedBy(variablesClass)
            } else {
                GRAPHQL_NO_VAR_OPERATION
            }

        val typeSpec =
            TypeSpec.objectBuilder(operation.name)
                .addModifiers(KModifier.PUBLIC)
                .addSuperinterface(superInterface)
                .addProperty(nameProperty(operation, operationsClass))
                .addProperty(documentProperty(operation, documentsClass))
                .addProperty(sha256HashProperty(operation, hashesClass))
                .apply {
                    if (hasVariables) {
                        addFunction(buildRequestFunction(operation, variablesClass, packageName, scalarMappings, schemaIndex))
                    }
                }
                .build()

        return FileSpec.builder(packageName, operation.name)
            .addType(typeSpec)
            .build()
    }

    private fun nameProperty(operation: GraphQLOperationInfo, operationsClass: ClassName) =
        com.squareup.kotlinpoet.PropertySpec.builder("name", String::class)
            .addModifiers(KModifier.OVERRIDE, KModifier.PUBLIC)
            .initializer(
                "%T.%L.%L",
                operationsClass,
                operation.type.name.lowercase().replaceFirstChar { it.uppercase() },
                operation.name,
            )
            .build()

    private fun documentProperty(operation: GraphQLOperationInfo, documentsClass: ClassName) =
        com.squareup.kotlinpoet.PropertySpec.builder("document", String::class)
            .addModifiers(KModifier.OVERRIDE, KModifier.PUBLIC)
            .initializer("%T.%L", documentsClass, operation.name)
            .build()

    private fun sha256HashProperty(operation: GraphQLOperationInfo, hashesClass: ClassName) =
        com.squareup.kotlinpoet.PropertySpec.builder("sha256Hash", String::class)
            .addModifiers(KModifier.OVERRIDE, KModifier.PUBLIC)
            .initializer("%T.%L", hashesClass, operation.name)
            .build()

    private fun buildRequestFunction(
        operation: GraphQLOperationInfo,
        variablesClass: ClassName,
        packageName: String,
        scalarMappings: Map<String, String>,
        schemaIndex: SchemaIndex,
    ): FunSpec {
        val params =
            operation.variables.map { variable ->
                val kotlinType = parseKotlinTypeString(variable.type, packageName, scalarMappings, schemaIndex)
                val paramBuilder = ParameterSpec.builder(variable.name, kotlinType)
                if (variable.defaultValue != null) {
                    paramBuilder.defaultValue(
                        GraphQLDefaultValueRenderer.render(
                            defaultValue = variable.defaultValue,
                            type = variable.type,
                            scalarMappings = scalarMappings,
                            schemaIndex = schemaIndex,
                        ),
                    )
                } else if (kotlinType.isNullable) {
                    paramBuilder.defaultValue(CodeBlock.of("null"))
                }
                paramBuilder.build()
            }

        // Build the variables constructor call: GetMarketPlaceAppsVariables(after = after, ...)
        val varArgs = operation.variables.joinToString(", ") { "${it.name} = ${it.name}" }
        val constructorCall = "%T($varArgs)"

        return FunSpec.builder("request")
            .addModifiers(KModifier.PUBLIC)
            .addParameters(params)
            .returns(GRAPHQL_OPERATION_REQUEST.parameterizedBy(variablesClass))
            .addStatement(
                "return %T(query = document, operationName = name, variables = $constructorCall)",
                GRAPHQL_OPERATION_REQUEST,
                variablesClass,
            )
            .build()
    }

    private fun parseKotlinTypeString(
        type: GraphQLType,
        packageName: String,
        scalarMappings: Map<String, String>,
        schemaIndex: SchemaIndex,
    ): com.squareup.kotlinpoet.TypeName {
        return GraphQLTypeMapper.toKotlinType(type, packageName, scalarMappings, schemaIndex)
    }
}
