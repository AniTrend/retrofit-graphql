package io.github.wax911.library.model

import io.github.wax911.library.model.attribute.GraphError

/**
 * Thrown when a GraphQL response contains errors and the caller requires
 * non-null data.
 *
 * @property errors The list of [GraphError] objects from the response.
 */
class GraphQLResponseException(
    val errors: List<GraphError>?,
) : RuntimeException(
    "GraphQL response contains errors: " +
        errors?.joinToString(separator = "; ", transform = GraphError::toString)
)
