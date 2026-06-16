package co.anitrend.retrofit.graphql.codegen.model

/**
 * Represents the type of a GraphQL operation.
 */
enum class OperationType {
    QUERY,
    MUTATION,
    SUBSCRIPTION;

    companion object {
        fun fromGraphQLJava(name: String): OperationType = when (name.lowercase()) {
            "query" -> QUERY
            "mutation" -> MUTATION
            "subscription" -> SUBSCRIPTION
            else -> throw IllegalArgumentException("Unknown operation type: $name")
        }
    }
}
