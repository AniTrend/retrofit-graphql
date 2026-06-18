package co.anitrend.retrofit.graphql.codegen.parser

import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.SchemaType
import graphql.language.EnumTypeDefinition
import graphql.language.InputObjectTypeDefinition
import graphql.language.InputValueDefinition
import graphql.language.ListType
import graphql.language.NonNullType
import graphql.language.ScalarTypeDefinition
import graphql.language.Type
import graphql.language.TypeName
import graphql.parser.Parser
import java.io.File

/**
 * Parses a GraphQL schema definition file (schema.graphql) and extracts
 * input object types, enum types, and scalar type definitions for code generation.
 */
class SchemaParser {

    private val parser = Parser()

    /**
     * Parses a schema file and returns all schema-level type definitions.
     *
     * @param file The schema.graphql file.
     * @return List of extracted [SchemaType] definitions.
     */
    fun parse(file: File): List<SchemaType> {
        val source = file.readText()
        val document = parser.parseDocument(source)

        val types = mutableListOf<SchemaType>()

        for (definition in document.definitions) {
            when (definition) {
                is InputObjectTypeDefinition -> {
                    types.add(parseInputObject(definition))
                }
                is EnumTypeDefinition -> {
                    types.add(parseEnum(definition))
                }
                is ScalarTypeDefinition -> {
                    types.add(SchemaType.Scalar(name = definition.name))
                }
                // Object types, interfaces, unions, schema definitions are ignored for now
            }
        }

        return types
    }

    /**
     * Converts a graphql-java [Type] AST node to our [GraphQLType] IR.
     */
    fun toGraphQLType(type: Type<*>): GraphQLType {
        return when (type) {
            is NonNullType -> {
                val inner = toGraphQLType(type.type)
                when (inner) {
                    is GraphQLType.Named -> inner.copy(nullable = false)
                    is GraphQLType.List -> inner.copy(nullable = false)
                }
            }
            is ListType -> {
                val elementType = toGraphQLType(type.type)
                GraphQLType.List(of = elementType, nullable = true)
            }
            is TypeName -> {
                val typeName = type.name
                    ?: throw IllegalArgumentException("TypeName must have a name")
                GraphQLType.Named(name = typeName, nullable = true)
            }
            else -> error("Unsupported type node: ${type.javaClass.simpleName}")
        }
    }

    private fun parseInputObject(definition: InputObjectTypeDefinition): SchemaType.InputObject {
        val fields = definition.inputValueDefinitions.map { field ->
            parseInputField(field)
        }
        return SchemaType.InputObject(
            name = definition.name,
            fields = fields,
        )
    }

    private fun parseInputField(field: InputValueDefinition): SchemaType.InputField {
        return SchemaType.InputField(
            name = field.name,
            type = toGraphQLType(field.type),
            defaultValue = field.defaultValue?.let { graphql.language.AstPrinter.printAst(it) },
        )
    }

    private fun parseEnum(definition: EnumTypeDefinition): SchemaType.Enum {
        val values = definition.enumValueDefinitions.map { it.name }
        return SchemaType.Enum(
            name = definition.name,
            values = values,
        )
    }
}
