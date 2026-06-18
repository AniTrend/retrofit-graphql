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
import co.anitrend.retrofit.graphql.codegen.model.GraphQLVariableInfo
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * Generates Kotlin data classes for operation variables.
 *
 * Example output:
 * ```kotlin
 * public data class GetMarketPlaceAppsVariables(
 *     public val after: String? = null,
 *     public val before: String? = null,
 *     public val first: Int,
 * ) : GraphQLVariables
 * ```
 */
object VariableClassGenerator {
    private val VARIABLES_INTERFACE = ClassName("co.anitrend.retrofit.graphql.model", "GraphQLVariables")

    /**
     * Generates a variable class for a single operation that has variables.
     * Returns null if the operation has no variables.
     */
    fun generate(
        operation: GraphQLOperationInfo,
        packageName: String,
        scalarMappings: Map<String, String>,
        schemaTypeNames: Set<String>,
    ): FileSpec? {
        if (operation.variables.isEmpty()) return null

        val className = "${operation.name}Variables"

        val typeSpec =
            TypeSpec.classBuilder(className)
                .addModifiers(KModifier.PUBLIC, KModifier.DATA)
                .addSuperinterface(VARIABLES_INTERFACE)
                .apply {
                    // Constructor parameters
                    val constructorParams =
                        operation.variables.map { variable ->
                            buildConstructorParam(variable, packageName, scalarMappings, schemaTypeNames)
                        }
                    primaryConstructor(
                        com.squareup.kotlinpoet.FunSpec.constructorBuilder()
                            .addParameters(constructorParams)
                            .build(),
                    )
                    // Properties
                    operation.variables.forEach { variable ->
                        addProperty(
                            PropertySpec.builder(
                                variable.name,
                                parseKotlinTypeString(variable.type, packageName, scalarMappings, schemaTypeNames),
                            )
                                .initializer(variable.name)
                                .addModifiers(KModifier.PUBLIC)
                                .build(),
                        )
                    }
                }
                .build()

        return FileSpec.builder(packageName, className)
            .addType(typeSpec)
            .build()
    }

    private fun buildConstructorParam(
        variable: GraphQLVariableInfo,
        packageName: String,
        scalarMappings: Map<String, String>,
        schemaTypeNames: Set<String>,
    ): ParameterSpec {
        val kotlinType = parseKotlinTypeString(variable.type, packageName, scalarMappings, schemaTypeNames)
        val paramBuilder = ParameterSpec.builder(variable.name, kotlinType)

        // Apply default value if present
        if (variable.defaultValue != null) {
            val defaultExpr = convertGraphQLDefaultToKotlin(variable.defaultValue, variable.type)
            paramBuilder.defaultValue(defaultExpr)
        }

        return paramBuilder.build()
    }

    private fun parseKotlinTypeString(
        type: GraphQLType,
        packageName: String,
        scalarMappings: Map<String, String>,
        schemaTypeNames: Set<String>,
    ): com.squareup.kotlinpoet.TypeName {
        return GraphQLTypeMapper.toKotlinType(type, packageName, scalarMappings, schemaTypeNames)
    }

    /**
     * Converts a GraphQL default value literal to a Kotlin expression.
     */
    private fun convertGraphQLDefaultToKotlin(
        defaultValue: String,
        type: GraphQLType,
    ): String {
        return when {
            defaultValue == "null" -> "null"
            defaultValue.startsWith("\"") -> defaultValue // String literal -- keep as-is
            defaultValue == "true" || defaultValue == "false" -> defaultValue
            defaultValue.toDoubleOrNull() != null -> defaultValue
            else -> "\"$defaultValue\"" // Enum-like value, wrap in quotes
        }
    }
}
