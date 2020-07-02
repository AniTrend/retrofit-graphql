package io.github.wax911.library.model.body

import io.github.wax911.library.model.attribute.GraphError

/**
 * GraphQL response that is spec complaint.
 *
 * @see [GraphQL Data Specification](http://spec.graphql.org/June2018/#sec-Data)
 */
data class GraphContainer<T>(
    val data: T? = null,
    val errors: List<GraphError>? = null,
    val extensions: Map<Any, Any>? = null
)
