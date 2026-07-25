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
import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import co.anitrend.retrofit.graphql.codegen.model.SchemaType
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
 * Generates Kotlin data classes for schema input object types.
 *
 * Backend-dependent annotations:
 * - [SerializationBackend.KOTLINX]: `@Serializable` on class, `@SerialName(wireName)` on every field.
 * - [SerializationBackend.GSON]: `@SerializedName(wireName)` on every field.
 * - [SerializationBackend.NONE]: No serialization annotations.
 *
 * Example output:
 * ```kotlin
 * @Serializable
 * public data class MediaSort(
 *     @SerialName("sort") public val sort: MediaSortEnum? = null,
 *     @SerialName("order") public val order: SortOrder? = null,
 * ) : GraphQLVariables
 * ```
 */
object InputObjectGenerator {
    private val VARIABLES_INTERFACE = ClassName("co.anitrend.retrofit.graphql.model", "GraphQLVariables")
    private val SERIALIZABLE = ClassName("kotlinx.serialization", "Serializable")
    private val SERIAL_NAME = ClassName("kotlinx.serialization", "SerialName")
    private val SERIALIZED_NAME = ClassName("com.google.gson.annotations", "SerializedName")

    /**
     * Generates a data class for each input object type in the schema.
     *
     * @param backend The serialization backend to use for annotation emission.
     */
    fun generate(
        inputObjects: List<SchemaType.InputObject>,
        packageName: String,
        scalarMappings: Map<String, String>,
        schemaIndex: SchemaIndex,
        backend: SerializationBackend = SerializationBackend.NONE,
    ): List<FileSpec> {
        return inputObjects.map { inputObject ->
            generateSingle(inputObject, packageName, scalarMappings, schemaIndex, backend)
        }
    }

    private fun generateSingle(
        inputObject: SchemaType.InputObject,
        packageName: String,
        scalarMappings: Map<String, String>,
        schemaIndex: SchemaIndex,
        backend: SerializationBackend,
    ): FileSpec {
        val allocator = GraphNameAllocator()

        // Pre-allocate all field names to avoid double-allocation
        val allocatedNames = inputObject.fields.associate { field ->
            field.name to allocator.allocatePropertyName(field.name)
        }

        val typeSpec =
            TypeSpec.classBuilder(inputObject.name)
                .addModifiers(KModifier.PUBLIC, KModifier.DATA)
                .addSuperinterface(VARIABLES_INTERFACE)
                .apply {
                    if (backend == SerializationBackend.KOTLINX) {
                        addAnnotation(SERIALIZABLE)
                    }
                    val params =
                        inputObject.fields.map { field ->
                            val allocated = allocatedNames[field.name]!!
                            val kotlinType = parseKotlinTypeString(field.type, packageName, scalarMappings, schemaIndex)
                            val paramBuilder = ParameterSpec.builder(allocated.kotlinName, kotlinType)
                            if (field.defaultValue != null) {
                                paramBuilder.defaultValue(
                                    GraphQLDefaultValueRenderer.render(
                                        defaultValue = field.defaultValue,
                                        type = field.type,
                                        scalarMappings = scalarMappings,
                                        schemaIndex = schemaIndex,
                                    ),
                                )
                            } else if (kotlinType.isNullable) {
                                paramBuilder.defaultValue(CodeBlock.of("null"))
                            }
                            paramBuilder.build()
                        }
                    primaryConstructor(
                        com.squareup.kotlinpoet.FunSpec.constructorBuilder()
                            .addParameters(params)
                            .build(),
                    )
                    // Properties
                    inputObject.fields.forEach { field ->
                        val allocated = allocatedNames[field.name]!!
                        val propBuilder =
                            PropertySpec.builder(
                                allocated.kotlinName,
                                parseKotlinTypeString(field.type, packageName, scalarMappings, schemaIndex),
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

        return FileSpec.builder(packageName, inputObject.name)
            .addType(typeSpec)
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
