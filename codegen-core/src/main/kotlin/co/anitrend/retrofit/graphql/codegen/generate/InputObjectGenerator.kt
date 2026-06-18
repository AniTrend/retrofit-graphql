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
import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.SchemaType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * Generates Kotlin data classes for schema input object types.
 *
 * Example output:
 * ```kotlin
 * public data class MediaSort(
 *     public val sort: MediaSortEnum? = null,
 *     public val order: SortOrder? = null,
 * ) : GraphQLVariables
 * ```
 */
object InputObjectGenerator {
    private val VARIABLES_INTERFACE = ClassName("co.anitrend.retrofit.graphql.model", "GraphQLVariables")

    /**
     * Generates a data class for each input object type in the schema.
     */
    fun generate(
        inputObjects: List<SchemaType.InputObject>,
        packageName: String,
        scalarMappings: Map<String, String>,
        schemaTypeNames: Set<String>,
    ): List<FileSpec> {
        return inputObjects.map { inputObject ->
            generateSingle(inputObject, packageName, scalarMappings, schemaTypeNames)
        }
    }

    private fun generateSingle(
        inputObject: SchemaType.InputObject,
        packageName: String,
        scalarMappings: Map<String, String>,
        schemaTypeNames: Set<String>,
    ): FileSpec {
        val typeSpec =
            TypeSpec.classBuilder(inputObject.name)
                .addModifiers(KModifier.PUBLIC, KModifier.DATA)
                .addSuperinterface(VARIABLES_INTERFACE)
                .apply {
                    val params =
                        inputObject.fields.map { field ->
                            val kotlinType = parseKotlinTypeString(field.type, packageName, scalarMappings, schemaTypeNames)
                            val paramBuilder = ParameterSpec.builder(field.name, kotlinType)
                            if (field.defaultValue != null) {
                                paramBuilder.defaultValue(
                                    convertGraphQLDefaultToKotlin(field.defaultValue, field.type),
                                )
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
                        addProperty(
                            PropertySpec.builder(
                                field.name,
                                parseKotlinTypeString(field.type, packageName, scalarMappings, schemaTypeNames),
                            )
                                .initializer(field.name)
                                .addModifiers(KModifier.PUBLIC)
                                .build(),
                        )
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
        schemaTypeNames: Set<String>,
    ): com.squareup.kotlinpoet.TypeName {
        return GraphQLTypeMapper.toKotlinType(type, packageName, scalarMappings, schemaTypeNames)
    }

    private fun convertGraphQLDefaultToKotlin(
        defaultValue: String,
        type: GraphQLType,
    ): String {
        return when {
            defaultValue == "null" -> "null"
            defaultValue.startsWith("\"") -> defaultValue
            defaultValue == "true" || defaultValue == "false" -> defaultValue
            defaultValue.toDoubleOrNull() != null -> defaultValue
            else -> "\"$defaultValue\""
        }
    }
}
