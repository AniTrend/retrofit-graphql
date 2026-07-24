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

package co.anitrend.retrofit.graphql.codegen.parser

import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.OutputField
import co.anitrend.retrofit.graphql.codegen.model.SchemaType
import graphql.language.EnumTypeDefinition
import graphql.language.FieldDefinition
import graphql.language.InputObjectTypeDefinition
import graphql.language.InputValueDefinition
import graphql.language.InterfaceTypeDefinition
import graphql.language.ListType
import graphql.language.NonNullType
import graphql.language.ObjectTypeDefinition
import graphql.language.ScalarTypeDefinition
import graphql.language.SchemaDefinition
import graphql.language.Type
import graphql.language.TypeName
import graphql.language.UnionTypeDefinition
import graphql.parser.Parser
import graphql.parser.ParserOptions
import java.io.File

/**
 * Parsed result from a GraphQL schema definition file, including type definitions
 * and root operation type names.
 *
 * @property types All schema-level type definitions (input objects, enums, scalars,
 *   object types, interfaces, and unions).
 * @property queryTypeName The name of the root query type, or null if not defined.
 * @property mutationTypeName The name of the root mutation type, or null if not defined.
 * @property subscriptionTypeName The name of the root subscription type, or null if not defined.
 */
data class SchemaParseResult(
    val types: List<SchemaType>,
    val queryTypeName: String? = null,
    val mutationTypeName: String? = null,
    val subscriptionTypeName: String? = null,
)

/**
 * Parses a GraphQL schema definition file (schema.graphql) and extracts
 * all schema-level type definitions (input object types, enum types, scalar types,
 * object types, interface types, and union types) for code generation.
 */
class SchemaParser {
    init {
        val options =
            ParserOptions.newParserOptions()
                .maxTokens(Int.MAX_VALUE)
                .build()
        ParserOptions.setDefaultParserOptions(options)
    }

    private val parser = Parser()

    /**
     * Parses a schema file and returns all schema-level type definitions.
     * Root operation type names from a [SchemaDefinition] are not included
     * in this result; use [parseWithRootTypes] if you need them.
     *
     * @param file The schema.graphql file.
     * @return List of extracted [SchemaType] definitions.
     */
    fun parse(file: File): List<SchemaType> {
        return parseWithRootTypes(file).types
    }

    /**
     * Parses a schema file and returns a [SchemaParseResult] containing all
     * schema-level type definitions and any root operation type names defined
     * by a [SchemaDefinition].
     *
     * @param file The schema.graphql file.
     * @return [SchemaParseResult] with type definitions and root type names.
     */
    fun parseWithRootTypes(file: File): SchemaParseResult {
        val source = file.readText()
        val document = parser.parseDocument(source)

        val types = mutableListOf<SchemaType>()
        var queryTypeName: String? = null
        var mutationTypeName: String? = null
        var subscriptionTypeName: String? = null

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
                is ObjectTypeDefinition -> {
                    types.add(parseObjectType(definition))
                }
                is InterfaceTypeDefinition -> {
                    types.add(parseInterfaceType(definition))
                }
                is UnionTypeDefinition -> {
                    types.add(parseUnionType(definition))
                }
                is SchemaDefinition -> {
                    definition.operationTypeDefinitions.forEach { opDef ->
                        val typeNameStr = opDef.typeName.name!!
                        when (opDef.name.lowercase()) {
                            "query" -> queryTypeName = typeNameStr
                            "mutation" -> mutationTypeName = typeNameStr
                            "subscription" -> subscriptionTypeName = typeNameStr
                        }
                    }
                }
            }
        }

        return SchemaParseResult(
            types = types,
            queryTypeName = queryTypeName ?: defaultRootTypeName(types, "Query"),
            mutationTypeName = mutationTypeName ?: defaultRootTypeName(types, "Mutation"),
            subscriptionTypeName = subscriptionTypeName ?: defaultRootTypeName(types, "Subscription"),
        )
    }

    /**
     * Returns the [name] if a type with that name exists in [types], null otherwise.
     * Implements the GraphQL convention of default root type names when no
     * [graphql.language.SchemaDefinition] is present.
     */
    private fun defaultRootTypeName(types: List<SchemaType>, name: String): String? {
        return if (types.any { it.name == name }) name else null
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
                val typeName =
                    type.name
                        ?: throw IllegalArgumentException("TypeName must have a name")
                GraphQLType.Named(name = typeName, nullable = true)
            }
            else -> error("Unsupported type node: ${type.javaClass.simpleName}")
        }
    }

    private fun parseInputObject(definition: InputObjectTypeDefinition): SchemaType.InputObject {
        val fields =
            definition.inputValueDefinitions.map { field ->
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

    private fun parseObjectType(definition: ObjectTypeDefinition): SchemaType.ObjectType {
        val fields = definition.fieldDefinitions.map { parseOutputField(it) }
        val interfaces =
            definition.implements
                .filterIsInstance<TypeName>()
                .map { it.name!! }
        return SchemaType.ObjectType(
            name = definition.name,
            fields = fields,
            interfaces = interfaces,
        )
    }

    private fun parseInterfaceType(definition: InterfaceTypeDefinition): SchemaType.InterfaceType {
        val fields = definition.fieldDefinitions.map { parseOutputField(it) }
        return SchemaType.InterfaceType(
            name = definition.name,
            fields = fields,
        )
    }

    private fun parseUnionType(definition: UnionTypeDefinition): SchemaType.UnionType {
        val memberTypes =
            definition.memberTypes
                .filterIsInstance<TypeName>()
                .map { it.name!! }
        return SchemaType.UnionType(
            name = definition.name,
            memberTypes = memberTypes,
        )
    }

    private fun parseOutputField(field: FieldDefinition): OutputField {
        val arguments =
            field.inputValueDefinitions.map { parseInputField(it) }
        return OutputField(
            name = field.name,
            type = toGraphQLType(field.type),
            arguments = arguments,
            description = field.description?.content,
        )
    }
}
