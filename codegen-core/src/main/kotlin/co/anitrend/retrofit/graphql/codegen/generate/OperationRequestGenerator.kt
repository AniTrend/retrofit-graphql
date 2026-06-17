package co.anitrend.retrofit.graphql.codegen.generate

import co.anitrend.retrofit.graphql.codegen.mapping.GraphQLTypeMapper
import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import com.squareup.kotlinpoet.ClassName
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
 *     public fun request(after: String? = null, before: String? = null, first: Int): GraphQLRequest<GetMarketPlaceAppsVariables> =
 *         GraphQLRequest(query = document, operationName = name, variables = GetMarketPlaceAppsVariables(...))
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
 */
object OperationRequestGenerator {

    private val GRAPHQL_OPERATION = ClassName("io.github.wax911.library.model", "GraphQLOperation")
    private val GRAPHQL_NO_VAR_OPERATION = ClassName("io.github.wax911.library.model", "GraphQLNoVarOperation")
    private val GRAPHQL_REQUEST = ClassName("io.github.wax911.library.model", "GraphQLRequest")
    private val OPERATIONS_CLASS = ClassName("", "GraphQLOperations")
    private val DOCUMENTS_CLASS = ClassName("", "GraphQLDocuments")
    private val HASHES_CLASS = ClassName("", "GraphQLHashes")

    /**
     * Generates a request helper object for a single operation.
     */
    fun generate(
        operation: GraphQLOperationInfo,
        packageName: String,
        scalarMappings: Map<String, String>,
        schemaTypeNames: Set<String>,
    ): FileSpec {
        val hasVariables = operation.variables.isNotEmpty()
        val variablesClassName = "${operation.name}Variables"
        val variablesClass = ClassName("", variablesClassName)

        val superInterface = if (hasVariables) {
            GRAPHQL_OPERATION.parameterizedBy(variablesClass)
        } else {
            GRAPHQL_NO_VAR_OPERATION
        }

        val typeSpec = TypeSpec.objectBuilder(operation.name)
            .addModifiers(KModifier.PUBLIC)
            .addSuperinterface(superInterface)
            .addProperty(nameProperty(operation))
            .addProperty(documentProperty(operation))
            .addProperty(sha256HashProperty(operation))
            .apply {
                if (hasVariables) {
                    addFunction(buildRequestFunction(operation, variablesClass, scalarMappings, schemaTypeNames))
                }
            }
            .build()

        return FileSpec.builder(packageName, operation.name)
            .addType(typeSpec)
            .build()
    }

    private fun nameProperty(operation: GraphQLOperationInfo) =
        com.squareup.kotlinpoet.PropertySpec.builder("name", String::class)
            .addModifiers(KModifier.OVERRIDE, KModifier.PUBLIC)
            .initializer(
                "%T.%L.%L",
                OPERATIONS_CLASS,
                operation.type.name.lowercase().replaceFirstChar { it.uppercase() },
                operation.name,
            )
            .build()

    private fun documentProperty(operation: GraphQLOperationInfo) =
        com.squareup.kotlinpoet.PropertySpec.builder("document", String::class)
            .addModifiers(KModifier.OVERRIDE, KModifier.PUBLIC)
            .initializer("%T.%L", DOCUMENTS_CLASS, operation.name)
            .build()

    private fun sha256HashProperty(operation: GraphQLOperationInfo) =
        com.squareup.kotlinpoet.PropertySpec.builder("sha256Hash", String::class)
            .addModifiers(KModifier.OVERRIDE, KModifier.PUBLIC)
            .initializer("%T.%L", HASHES_CLASS, operation.name)
            .build()

    private fun buildRequestFunction(
        operation: GraphQLOperationInfo,
        variablesClass: ClassName,
        scalarMappings: Map<String, String>,
        schemaTypeNames: Set<String>,
    ): FunSpec {
        val params = operation.variables.map { variable ->
            val kotlinType = parseKotlinTypeString(variable.type, scalarMappings, schemaTypeNames)
            val paramBuilder = ParameterSpec.builder(variable.name, kotlinType)
            if (variable.defaultValue != null) {
                paramBuilder.defaultValue(
                    convertGraphQLDefaultToKotlin(variable.defaultValue, variable.type)
                )
            }
            paramBuilder.build()
        }

        // Build the variables constructor call: GetMarketPlaceAppsVariables(after = after, ...)
        val varArgs = operation.variables.joinToString(", ") { "${it.name} = ${it.name}" }
        val constructorCall = "%T($varArgs)"

        return FunSpec.builder("request")
            .addModifiers(KModifier.PUBLIC)
            .addParameters(params)
            .returns(GRAPHQL_REQUEST.parameterizedBy(variablesClass))
            .addStatement(
                "return %T(query = document, operationName = name, variables = $constructorCall)",
                GRAPHQL_REQUEST,
                variablesClass,
            )
            .build()
    }

    private fun parseKotlinTypeString(
        type: GraphQLType,
        scalarMappings: Map<String, String>,
        schemaTypeNames: Set<String>,
    ): com.squareup.kotlinpoet.TypeName {
        val typeStr = GraphQLTypeMapper.toKotlinType(type, scalarMappings, schemaTypeNames)
        return parseTypeString(typeStr)
    }

    private fun parseTypeString(typeStr: String): com.squareup.kotlinpoet.TypeName {
        val nullable = typeStr.endsWith("?")
        val base = if (nullable) typeStr.dropLast(1) else typeStr

        return when (base) {
            "kotlin.String" -> ClassName("kotlin", "String").copy(nullable = nullable)
            "kotlin.Int" -> ClassName("kotlin", "Int").copy(nullable = nullable)
            "kotlin.Double" -> ClassName("kotlin", "Double").copy(nullable = nullable)
            "kotlin.Boolean" -> ClassName("kotlin", "Boolean").copy(nullable = nullable)
            "kotlin.Float" -> ClassName("kotlin", "Float").copy(nullable = nullable)
            else -> ClassName("", base).copy(nullable = nullable)
        }
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
