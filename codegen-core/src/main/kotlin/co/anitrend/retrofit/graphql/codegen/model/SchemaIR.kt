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

package co.anitrend.retrofit.graphql.codegen.model

/**
 * Internal representation of a schema-level type definition.
 *
 * Covers all schema type kinds: input objects, enums, scalars,
 * output object types, interface types, and union types.
 */
sealed class SchemaType {
    /**
     * The schema type name.
     */
    abstract val name: String

    /**
     * An input object type defined in the schema.
     *
     * @param name The type name.
     * @param fields The input fields with their GraphQL types.
     */
    data class InputObject(
        override val name: String,
        val fields: List<InputField>,
    ) : SchemaType()

    /**
     * An enum type defined in the schema.
     *
     * @param name The type name.
     * @param values The enum value names.
     */
    data class Enum(
        override val name: String,
        val values: List<String>,
    ) : SchemaType()

    /**
     * A scalar type defined in the schema (custom scalar).
     *
     * @param name The type name.
     */
    data class Scalar(
        override val name: String,
    ) : SchemaType()

    /**
     * An output object type defined in the schema.
     *
     * @param name The type name.
     * @param fields The output fields defined on this object type.
     * @param interfaces The names of interfaces implemented by this object type.
     */
    data class ObjectType(
        override val name: String,
        val fields: List<OutputField>,
        val interfaces: List<String> = emptyList(),
    ) : SchemaType()

    /**
     * An interface type defined in the schema.
     *
     * @param name The type name.
     * @param fields The output fields defined on this interface type.
     * @param possibleTypes The names of concrete object types that implement this interface.
     */
    data class InterfaceType(
        override val name: String,
        val fields: List<OutputField>,
        val possibleTypes: List<String> = emptyList(),
    ) : SchemaType()

    /**
     * A union type defined in the schema.
     *
     * @param name The type name.
     * @param memberTypes The names of the member types of this union.
     */
    data class UnionType(
        override val name: String,
        val memberTypes: List<String>,
    ) : SchemaType()

    /**
     * A single field within an input object.
     *
     * @param name The field name.
     * @param type The GraphQL type of the field.
     * @param defaultValue The default value literal, or null.
     */
    data class InputField(
        val name: String,
        val type: GraphQLType,
        val defaultValue: String? = null,
    )
}

/**
 * A single output field on an object or interface type.
 * Different from [SchemaType.InputField]: output fields can have arguments.
 *
 * @param name The field name.
 * @param type The GraphQL type of the field's return value.
 * @param arguments The input arguments this field accepts, or empty if none.
 * @param description The field's description from the schema, or null.
 */
data class OutputField(
    val name: String,
    val type: GraphQLType,
    val arguments: List<SchemaType.InputField> = emptyList(),
    val description: String? = null,
)
