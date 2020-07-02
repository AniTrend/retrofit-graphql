package io.github.wax911.library.model.attribute

/**
 * GraphQL error representation that is spec complaint
 *
 * @param message Description of the error.
 * @param path Path of the the response field that encountered the error, as segments that
 * represent fields should be strings, and path segments that represent list indices
 * should be 0‐indexed integers. If the error happens in an aliased field, the path to the
 * error should use the aliased name, since it represents a path in the response, not in the query.
 * @param locations List of locations within the GraphQL document at which the exception occurred.
 * @param extensions Additional information about the error.
 *
 * @see [GraphQL Error Specification](http://spec.graphql.org/June2018/#sec-Errors)
 */
data class GraphError(
    val message: String?,
    val path: List<Any>? = null,
    val locations: List<Location>? = null,
    val extensions: Map<String, Any?>? = null
) {

    /**
     * Location describing which part of GraphQL document caused an exception.
     */
    data class Location(
        val line: Int,
        val column: Int
    )

    override fun toString(): String {
        return "GraphError{" +
                "message='" + message + '\''.toString() +
                ", path=" + path?.joinToString() +
                ", locations=" + locations?.joinToString() +
                ", extensions=" + extensions +
                '}'.toString()
    }
}
