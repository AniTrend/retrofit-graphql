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

    private val VARIABLES_INTERFACE = ClassName("io.github.wax911.library.model", "GraphQLVariables")

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

        val typeSpec = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.PUBLIC, KModifier.DATA)
            .addSuperinterface(VARIABLES_INTERFACE)
            .apply {
                // Constructor parameters
                val constructorParams = operation.variables.map { variable ->
                    buildConstructorParam(variable, scalarMappings, schemaTypeNames)
                }
                primaryConstructor(
                    com.squareup.kotlinpoet.FunSpec.constructorBuilder()
                        .addParameters(constructorParams)
                        .build()
                )
                // Properties
                operation.variables.forEach { variable ->
                    addProperty(
                        PropertySpec.builder(
                            variable.name,
                            parseKotlinTypeString(variable.type, scalarMappings, schemaTypeNames)
                        )
                            .initializer(variable.name)
                            .addModifiers(KModifier.PUBLIC)
                            .build()
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
        scalarMappings: Map<String, String>,
        schemaTypeNames: Set<String>,
    ): ParameterSpec {
        val kotlinType = parseKotlinTypeString(variable.type, scalarMappings, schemaTypeNames)
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
        scalarMappings: Map<String, String>,
        schemaTypeNames: Set<String>,
    ): com.squareup.kotlinpoet.TypeName {
        val typeStr = GraphQLTypeMapper.toKotlinType(type, scalarMappings, schemaTypeNames)
        return parseTypeString(typeStr)
    }

    /**
     * Parses a Kotlin type string like "kotlin.String?" into a KotlinPoet TypeName.
     */
    private fun parseTypeString(typeStr: String): com.squareup.kotlinpoet.TypeName {
        val nullable = typeStr.endsWith("?")
        val base = if (nullable) typeStr.dropLast(1) else typeStr

        return when (base) {
            "kotlin.String" -> ClassName("kotlin", "String").copy(nullable = nullable)
            "kotlin.Int" -> ClassName("kotlin", "Int").copy(nullable = nullable)
            "kotlin.Double" -> ClassName("kotlin", "Double").copy(nullable = nullable)
            "kotlin.Boolean" -> ClassName("kotlin", "Boolean").copy(nullable = nullable)
            "kotlin.Float" -> ClassName("kotlin", "Float").copy(nullable = nullable)
            else -> {
                // Custom type (input object, enum, or mapped scalar) -- use simple class name
                ClassName("", base).copy(nullable = nullable)
            }
        }
    }

    /**
     * Converts a GraphQL default value literal to a Kotlin expression.
     */
    private fun convertGraphQLDefaultToKotlin(defaultValue: String, type: GraphQLType): String {
        return when {
            defaultValue == "null" -> "null"
            defaultValue.startsWith("\"") -> defaultValue // String literal -- keep as-is
            defaultValue == "true" || defaultValue == "false" -> defaultValue
            defaultValue.toDoubleOrNull() != null -> defaultValue
            else -> "\"$defaultValue\"" // Enum-like value, wrap in quotes
        }
    }
}
