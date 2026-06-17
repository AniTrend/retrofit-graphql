package co.anitrend.retrofit.graphql.codegen.model

/**
 * Internal representation of a schema-level type definition.
 */
sealed class SchemaType {

    /**
     * An input object type defined in the schema.
     *
     * @param name The type name.
     * @param fields The input fields with their GraphQL types.
     */
    data class InputObject(
        val name: String,
        val fields: List<InputField>,
    ) : SchemaType()

    /**
     * An enum type defined in the schema.
     *
     * @param name The type name.
     * @param values The enum value names.
     */
    data class Enum(
        val name: String,
        val values: List<String>,
    ) : SchemaType()

    /**
     * A scalar type defined in the schema (custom scalar).
     *
     * @param name The type name.
     */
    data class Scalar(
        val name: String,
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
