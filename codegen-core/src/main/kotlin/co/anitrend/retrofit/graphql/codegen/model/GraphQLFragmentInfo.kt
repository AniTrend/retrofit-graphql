package co.anitrend.retrofit.graphql.codegen.model

/**
 * Parsed information about a GraphQL fragment definition.
 *
 * @param name The fragment name, e.g. "UserFields".
 * @param document The source text of the fragment definition.
 */
data class GraphQLFragmentInfo(
    val name: String,
    val document: String,
)
