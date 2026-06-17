package co.anitrend.retrofit.graphql.codegen.model

/**
 * Represents a parsed GraphQL type reference from a variable or field definition.
 */
sealed class GraphQLType {

    /**
     * A named type reference, e.g. "String", "MyInput", "DateTime".
     * @param nullable Whether the type is nullable (no trailing !).
     */
    data class Named(val name: String, val nullable: Boolean = true) : GraphQLType()

    /**
     * A list type reference, e.g. "[String!]!", "[MyInput]".
     * @param of The element type.
     * @param nullable Whether the list itself is nullable.
     */
    data class List(val of: GraphQLType, val nullable: Boolean = true) : GraphQLType()
}

/**
 * Parsed information about a single GraphQL operation variable.
 *
 * @param name The variable name (e.g. "after", "first").
 * @param type The GraphQL type of the variable.
 * @param defaultValue The default value as a GraphQL literal string, or null.
 */
data class GraphQLVariableInfo(
    val name: String,
    val type: GraphQLType,
    val defaultValue: String? = null,
)
