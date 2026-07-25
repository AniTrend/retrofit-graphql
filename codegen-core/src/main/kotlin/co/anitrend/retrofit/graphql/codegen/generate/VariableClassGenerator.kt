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

import co.anitrend.retrofit.graphql.codegen.config.SerializationBackend
import co.anitrend.retrofit.graphql.codegen.mapping.GraphQLTypeMapper
import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.GraphQLVariableInfo
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import co.anitrend.retrofit.graphql.codegen.naming.GeneratedName
import co.anitrend.retrofit.graphql.codegen.naming.GraphNameAllocator
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * Generates Kotlin data classes for operation variables.
 *
 * Backend-dependent annotations:
 * - [SerializationBackend.KOTLINX]: `@Serializable` on class, `@SerialName(wireName)` on every property.
 * - [SerializationBackend.GSON]: `@SerializedName(wireName)` on every property.
 * - [SerializationBackend.NONE]: No serialization annotations.
 *
 * Example output:
 * ```kotlin
 * @Serializable
 * public data class GetMarketPlaceAppsVariables(
 *     @SerialName("after") public val after: String? = null,
 *     @SerialName("before") public val before: String? = null,
 *     @SerialName("first") public val first: Int,
 * ) : GraphQLVariables
 * ```
 */
object VariableClassGenerator {
    private val VARIABLES_INTERFACE = ClassName("co.anitrend.retrofit.graphql.model", "GraphQLVariables")
    private val SERIALIZABLE = ClassName("kotlinx.serialization", "Serializable")
    private val SERIAL_NAME = ClassName("kotlinx.serialization", "SerialName")
    private val SERIALIZED_NAME = ClassName("com.google.gson.annotations", "SerializedName")

    /**
     * Generates a variable class for a single operation that has variables.
     * Returns null if the operation has no variables.
     *
     * @param backend The serialization backend to use for annotation emission.
     */
    fun generate(
        operation: GraphQLOperationInfo,
        packageName: String,
        scalarMappings: Map<String, String>,
        schemaIndex: SchemaIndex,
        backend: SerializationBackend = SerializationBackend.NONE,
    ): FileSpec? {
        if (operation.variables.isEmpty()) return null

        val className = "${operation.name}Variables"
        val allocator = GraphNameAllocator()

        // Pre-allocate all variable names to avoid double-allocation
        val allocatedNames = operation.variables.associate { variable ->
            variable.name to allocator.allocatePropertyName(variable.name)
        }

        val typeSpec =
            TypeSpec.classBuilder(className)
                .addModifiers(KModifier.PUBLIC, KModifier.DATA)
                .addSuperinterface(VARIABLES_INTERFACE)
                .apply {
                    if (backend == SerializationBackend.KOTLINX) {
                        addAnnotation(SERIALIZABLE)
                    }
                    // Constructor parameters
                    val constructorParams =
                        operation.variables.map { variable ->
                            val allocated = allocatedNames[variable.name]!!
                            buildConstructorParam(variable, allocated, packageName, scalarMappings, schemaIndex)
                        }
                    primaryConstructor(
                        com.squareup.kotlinpoet.FunSpec.constructorBuilder()
                            .addParameters(constructorParams)
                            .build(),
                    )
                    // Properties
                    operation.variables.forEach { variable ->
                        val allocated = allocatedNames[variable.name]!!
                        val propBuilder =
                            PropertySpec.builder(
                                allocated.kotlinName,
                                parseKotlinTypeString(variable.type, packageName, scalarMappings, schemaIndex),
                            )
                                .initializer(allocated.kotlinName)
                                .addModifiers(KModifier.PUBLIC)
                        when (backend) {
                            SerializationBackend.KOTLINX ->
                                propBuilder.addAnnotation(
                                    AnnotationSpec.builder(SERIAL_NAME)
                                        .addMember("%S", allocated.wireName)
                                        .build(),
                                )
                            SerializationBackend.GSON ->
                                propBuilder.addAnnotation(
                                    AnnotationSpec.builder(SERIALIZED_NAME)
                                        .addMember("%S", allocated.wireName)
                                        .build(),
                                )
                            else -> {}
                        }
                        addProperty(propBuilder.build())
                    }
                }
                .build()

        return FileSpec.builder(packageName, className)
            .addType(typeSpec)
            .build()
    }

    private fun buildConstructorParam(
        variable: GraphQLVariableInfo,
        allocated: GeneratedName,
        packageName: String,
        scalarMappings: Map<String, String>,
        schemaIndex: SchemaIndex,
    ): ParameterSpec {
        val kotlinType = parseKotlinTypeString(variable.type, packageName, scalarMappings, schemaIndex)
        val paramBuilder = ParameterSpec.builder(allocated.kotlinName, kotlinType)

        // Apply default value if present, or provide implicit null for nullable params
        if (variable.defaultValue != null) {
            val defaultExpr =
                GraphQLDefaultValueRenderer.render(
                    defaultValue = variable.defaultValue,
                    type = variable.type,
                    scalarMappings = scalarMappings,
                    schemaIndex = schemaIndex,
                )
            paramBuilder.defaultValue(defaultExpr)
        } else if (kotlinType.isNullable) {
            paramBuilder.defaultValue(CodeBlock.of("null"))
        }

        return paramBuilder.build()
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
