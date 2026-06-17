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
        val typeSpec = TypeSpec.classBuilder(inputObject.name)
            .addModifiers(KModifier.PUBLIC, KModifier.DATA)
            .addSuperinterface(VARIABLES_INTERFACE)
            .apply {
                val params = inputObject.fields.map { field ->
                    val kotlinType = parseKotlinTypeString(field.type, scalarMappings, schemaTypeNames)
                    val paramBuilder = ParameterSpec.builder(field.name, kotlinType)
                    if (field.defaultValue != null) {
                        paramBuilder.defaultValue(
                            convertGraphQLDefaultToKotlin(field.defaultValue, field.type)
                        )
                    }
                    paramBuilder.build()
                }
                primaryConstructor(
                    com.squareup.kotlinpoet.FunSpec.constructorBuilder()
                        .addParameters(params)
                        .build()
                )
                // Properties
                inputObject.fields.forEach { field ->
                    addProperty(
                        PropertySpec.builder(
                            field.name,
                            parseKotlinTypeString(field.type, scalarMappings, schemaTypeNames)
                        )
                            .initializer(field.name)
                            .addModifiers(KModifier.PUBLIC)
                            .build()
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
        scalarMappings: Map<String, String>,
        schemaTypeNames: Set<String>,
    ): com.squareup.kotlinpoet.TypeName {
        return GraphQLTypeMapper.toKotlinType(type, scalarMappings, schemaTypeNames)
    }

    private fun convertGraphQLDefaultToKotlin(defaultValue: String, type: GraphQLType): String {
        return when {
            defaultValue == "null" -> "null"
            defaultValue.startsWith("\"") -> defaultValue
            defaultValue == "true" || defaultValue == "false" -> defaultValue
            defaultValue.toDoubleOrNull() != null -> defaultValue
            else -> "\"$defaultValue\""
        }
    }
}
