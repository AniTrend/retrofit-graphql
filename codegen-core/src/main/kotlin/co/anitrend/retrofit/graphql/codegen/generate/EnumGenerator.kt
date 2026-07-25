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
import co.anitrend.retrofit.graphql.codegen.model.SchemaType
import co.anitrend.retrofit.graphql.codegen.naming.GraphNameAllocator
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * Generates Kotlin enum classes for GraphQL enum types.
 *
 * Active generation emits standalone Kotlin enum classes so generated variable,
 * input object, and request helper types can reference real Kotlin types.
 *
 * Backend-dependent annotations:
 * - [SerializationBackend.KOTLINX]: `@Serializable` on class, `@SerialName(wireName)` on each constant.
 * - [SerializationBackend.GSON]: `@SerializedName(wireName)` on each constant.
 * - [SerializationBackend.NONE]: No serialization annotations.
 *
 * Legacy string constant generation is still available for compatibility:
 * ```kotlin
 * public enum class MediaSort {
 *     @SerialName("SCORE") SCORE,
 *     @SerialName("POPULARITY") POPULARITY,
 * }
 * ```
 */
object EnumGenerator {
    private val SERIALIZABLE = ClassName("kotlinx.serialization", "Serializable")
    private val SERIAL_NAME = ClassName("kotlinx.serialization", "SerialName")
    private val SERIALIZED_NAME = ClassName("com.google.gson.annotations", "SerializedName")

    /**
     * Generates standalone Kotlin enum classes for a list of enum types.
     *
     * @param backend The serialization backend to use for annotation emission.
     */
    fun generate(
        enums: List<SchemaType.Enum>,
        packageName: String,
        backend: SerializationBackend = SerializationBackend.NONE,
    ): List<FileSpec> {
        return enums.map { enumType ->
            generateAsEnumClass(enumType, packageName, backend)
        }
    }

    /**
     * Generates string constants for a list of enum types.
     * All enums are grouped into a single `GraphQLEnums` file.
     */
    fun generateAsStringConstants(
        enums: List<SchemaType.Enum>,
        packageName: String,
    ): FileSpec? {
        if (enums.isEmpty()) return null

        val outerSpec =
            TypeSpec.objectBuilder("GraphQLEnums")
                .addModifiers(KModifier.PUBLIC)
                .apply {
                    enums.forEach { enumType ->
                        addType(buildEnumObject(enumType))
                    }
                }
                .build()

        return FileSpec.builder(packageName, "GraphQLEnums")
            .addType(outerSpec)
            .build()
    }

    /**
     * Generates a standalone Kotlin enum class for a single enum type.
     *
     * @param backend The serialization backend to use for annotation emission.
     */
    fun generateAsEnumClass(
        enumType: SchemaType.Enum,
        packageName: String,
        backend: SerializationBackend = SerializationBackend.NONE,
    ): FileSpec {
        val allocator = GraphNameAllocator()

        val typeSpec =
            TypeSpec.enumBuilder(enumType.name)
                .addModifiers(KModifier.PUBLIC)
                .apply {
                    if (backend == SerializationBackend.KOTLINX) {
                        addAnnotation(SERIALIZABLE)
                    }
                    enumType.values.forEach { value ->
                        val allocated = allocator.allocateEnumConstant(value)
                        val constantBody = TypeSpec.anonymousClassBuilder()
                        when (backend) {
                            SerializationBackend.KOTLINX ->
                                constantBody.addAnnotation(
                                    AnnotationSpec.builder(SERIAL_NAME)
                                        .addMember("%S", allocated.wireName)
                                        .build(),
                                )
                            SerializationBackend.GSON ->
                                constantBody.addAnnotation(
                                    AnnotationSpec.builder(SERIALIZED_NAME)
                                        .addMember("%S", allocated.wireName)
                                        .build(),
                                )
                            else -> {}
                        }
                        addEnumConstant(allocated.kotlinName, constantBody.build())
                    }
                }
                .build()

        return FileSpec.builder(packageName, enumType.name)
            .addType(typeSpec)
            .build()
    }

    private fun buildEnumObject(enumType: SchemaType.Enum): TypeSpec {
        return TypeSpec.objectBuilder(enumType.name)
            .addModifiers(KModifier.PUBLIC)
            .apply {
                enumType.values.forEach { value ->
                    addProperty(
                        PropertySpec.builder(value, String::class)
                            .addModifiers(KModifier.PUBLIC, KModifier.CONST)
                            .initializer("%S", value)
                            .build(),
                    )
                }
            }
            .build()
    }
}
