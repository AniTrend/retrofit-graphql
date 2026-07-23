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

import co.anitrend.retrofit.graphql.codegen.model.GraphQLType
import co.anitrend.retrofit.graphql.codegen.model.SchemaIndex
import co.anitrend.retrofit.graphql.codegen.model.SchemaType
import com.squareup.kotlinpoet.CodeBlock
import graphql.language.ArrayValue
import graphql.language.AstPrinter
import graphql.language.BooleanValue
import graphql.language.EnumValue
import graphql.language.FloatValue
import graphql.language.IntValue
import graphql.language.NullValue
import graphql.language.ObjectValue
import graphql.language.StringValue
import graphql.language.Value
import graphql.parser.Parser

/**
 * Renders GraphQL default literals as Kotlin expressions that match generated helper types.
 */
object GraphQLDefaultValueRenderer {
    private val BUILT_IN_SCALARS = setOf("String", "Int", "Float", "Boolean", "ID")

    /**
     * Converts a GraphQL literal string into a Kotlin expression [CodeBlock].
     */
    fun render(
        defaultValue: String,
        type: GraphQLType,
        scalarMappings: Map<String, String>,
        schemaIndex: SchemaIndex,
    ): CodeBlock {
        val parsedValue = Parser.parseValue(defaultValue)
        return renderValue(parsedValue, type, scalarMappings, schemaIndex, path = "default value")
    }

    private fun renderValue(
        value: Value<*>,
        type: GraphQLType,
        scalarMappings: Map<String, String>,
        schemaIndex: SchemaIndex,
        path: String,
    ): CodeBlock {
        if (value is NullValue) return CodeBlock.of("null")

        return when (type) {
            is GraphQLType.List -> renderListValue(value, type, scalarMappings, schemaIndex, path)
            is GraphQLType.Named -> renderNamedValue(value, type.name, scalarMappings, schemaIndex, path)
        }
    }

    private fun renderListValue(
        value: Value<*>,
        type: GraphQLType.List,
        scalarMappings: Map<String, String>,
        schemaIndex: SchemaIndex,
        path: String,
    ): CodeBlock {
        val elements = if (value is ArrayValue) value.values else listOf(value)
        val renderedElements =
            elements.mapIndexed { index, element ->
                renderValue(element, type.of, scalarMappings, schemaIndex, "$path[$index]")
            }

        return CodeBlock.builder()
            .add("listOf(")
            .apply {
                renderedElements.forEachIndexed { index, element ->
                    if (index > 0) add(", ")
                    add("%L", element)
                }
            }
            .add(")")
            .build()
    }

    private fun renderNamedValue(
        value: Value<*>,
        typeName: String,
        scalarMappings: Map<String, String>,
        schemaIndex: SchemaIndex,
        path: String,
    ): CodeBlock {
        scalarMappings[typeName]?.let { mappedType ->
            return renderMappedScalar(value, typeName, mappedType, path)
        }

        if (typeName in BUILT_IN_SCALARS) {
            return renderBuiltInScalar(value, typeName, path)
        }

        return when (val definition = schemaIndex.definition(typeName)) {
            is SchemaType.Enum -> renderEnumValue(value, definition.name, path)
            is SchemaType.InputObject -> renderInputObjectValue(value, definition, scalarMappings, schemaIndex, path)
            is SchemaType.Scalar -> {
                throw IllegalArgumentException(
                    "Cannot render $path for schema scalar '$typeName' without an explicit scalar mapping.",
                )
            }
            is SchemaType.ObjectType,
            is SchemaType.InterfaceType,
            is SchemaType.UnionType,
            null,
            -> {
                throw IllegalArgumentException(
                    "Cannot render $path for unknown type '$typeName'. " +
                        "Add a scalar mapping in the retrofitGraphQL {} extension, e.g.:\n" +
                        "  scalars { map(\"$typeName\", \"kotlin.String\") }",
                )
            }
        }
    }

    private fun renderInputObjectValue(
        value: Value<*>,
        inputObject: SchemaType.InputObject,
        scalarMappings: Map<String, String>,
        schemaIndex: SchemaIndex,
        path: String,
    ): CodeBlock {
        require(value is ObjectValue) {
            "Expected an input object literal for $path of type '${inputObject.name}', but found ${value.javaClass.simpleName}."
        }

        val fieldsByName = inputObject.fields.associateBy { it.name }

        return CodeBlock.builder()
            .add("%L(", inputObject.name)
            .apply {
                value.objectFields.forEachIndexed { index, field ->
                    val fieldDefinition =
                        requireNotNull(fieldsByName[field.name]) {
                            "Unknown field '${field.name}' in $path for input object '${inputObject.name}'."
                        }
                    if (index > 0) add(", ")
                    add(
                        "%L = %L",
                        field.name,
                        renderValue(
                            value = field.value,
                            type = fieldDefinition.type,
                            scalarMappings = scalarMappings,
                            schemaIndex = schemaIndex,
                            path = "$path.${field.name}",
                        ),
                    )
                }
            }
            .add(")")
            .build()
    }

    private fun renderEnumValue(
        value: Value<*>,
        enumTypeName: String,
        path: String,
    ): CodeBlock {
        require(value is EnumValue) {
            "Expected an enum literal for $path of type '$enumTypeName', but found ${value.javaClass.simpleName}."
        }
        return CodeBlock.of("%L.%L", enumTypeName, value.name)
    }

    private fun renderBuiltInScalar(
        value: Value<*>,
        typeName: String,
        path: String,
    ): CodeBlock {
        return when (typeName) {
            "String" -> renderStrictString(value, path)
            "ID" -> renderId(value, path)
            "Int" -> renderInteger(value, path)
            "Float" -> renderDouble(value, path)
            "Boolean" -> renderBoolean(value, path)
            else -> error("Unsupported built-in scalar '$typeName'")
        }
    }

    private fun renderMappedScalar(
        value: Value<*>,
        graphQLTypeName: String,
        mappedType: String,
        path: String,
    ): CodeBlock {
        return when (mappedType.removePrefix("kotlin.")) {
            "String" -> renderStringCompatible(value)
            "Int" -> renderInteger(value, path)
            "Long" -> renderLong(value, path)
            "Double" -> renderDouble(value, path)
            "Float" -> renderFloat(value, path)
            "Boolean" -> renderBoolean(value, path)
            else -> {
                throw IllegalArgumentException(
                    "Cannot render $path for scalar '$graphQLTypeName' mapped to '$mappedType'. " +
                        "Only Kotlin String, Int, Long, Float, Double, and Boolean mappings can be rendered automatically.",
                )
            }
        }
    }

    private fun renderStrictString(
        value: Value<*>,
        path: String,
    ): CodeBlock {
        require(value is StringValue) {
            "Expected a string literal for $path, but found ${value.javaClass.simpleName}."
        }
        return CodeBlock.of("%S", value.value)
    }

    private fun renderStringCompatible(value: Value<*>): CodeBlock {
        return when (value) {
            is StringValue -> CodeBlock.of("%S", value.value)
            else -> CodeBlock.of("%S", AstPrinter.printAst(value))
        }
    }

    private fun renderId(
        value: Value<*>,
        path: String,
    ): CodeBlock {
        return when (value) {
            is StringValue -> CodeBlock.of("%S", value.value)
            is IntValue -> CodeBlock.of("%S", value.value.toString())
            else -> throw IllegalArgumentException(
                "Expected a string or integer literal for $path, but found ${value.javaClass.simpleName}.",
            )
        }
    }

    private fun renderInteger(
        value: Value<*>,
        path: String,
    ): CodeBlock {
        require(value is IntValue) {
            "Expected an integer literal for $path, but found ${value.javaClass.simpleName}."
        }
        return CodeBlock.of("%L", value.value.toString())
    }

    private fun renderLong(
        value: Value<*>,
        path: String,
    ): CodeBlock {
        require(value is IntValue) {
            "Expected an integer literal for $path, but found ${value.javaClass.simpleName}."
        }
        return CodeBlock.of("%LL", value.value.toString())
    }

    private fun renderDouble(
        value: Value<*>,
        path: String,
    ): CodeBlock {
        return when (value) {
            is FloatValue -> CodeBlock.of("%L", normalizeDecimal(value.value.toPlainString()))
            is IntValue -> CodeBlock.of("%L", "${value.value}.0")
            else -> throw IllegalArgumentException(
                "Expected a numeric literal for $path, but found ${value.javaClass.simpleName}.",
            )
        }
    }

    private fun renderFloat(
        value: Value<*>,
        path: String,
    ): CodeBlock {
        return when (value) {
            is FloatValue -> CodeBlock.of("%Lf", normalizeDecimal(value.value.toPlainString()))
            is IntValue -> CodeBlock.of("%Lf", "${value.value}.0")
            else -> throw IllegalArgumentException(
                "Expected a numeric literal for $path, but found ${value.javaClass.simpleName}.",
            )
        }
    }

    private fun renderBoolean(
        value: Value<*>,
        path: String,
    ): CodeBlock {
        require(value is BooleanValue) {
            "Expected a boolean literal for $path, but found ${value.javaClass.simpleName}."
        }
        return CodeBlock.of("%L", value.isValue)
    }

    private fun normalizeDecimal(value: String): String {
        return if ('.' in value || 'e' in value.lowercase()) value else "$value.0"
    }
}
