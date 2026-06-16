package co.anitrend.retrofit.graphql.codegen.model

/**
 * Parsed information about a single GraphQL operation (query, mutation, or subscription).
 *
 * @param name The operation name, e.g. "GetCurrentUser".
 * @param type Whether this is a query, mutation, or subscription.
 * @param document The full source text of this operation, with fragments inlined.
 * @param sourceFile The file path from which this operation was parsed.
 */
data class GraphQLOperationInfo(
    val name: String,
    val type: OperationType,
    val document: String,
    val sourceFile: String,
)
