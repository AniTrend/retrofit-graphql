package io.github.wax911.library.model.body

import io.github.wax911.library.model.attribute.GraphError

data class GraphContainer<T>(
        val data: T?,
        val errors: List<GraphError>?
) { fun isEmpty(): Boolean = data == null }
