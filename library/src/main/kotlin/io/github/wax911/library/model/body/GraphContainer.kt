@file:Suppress("DEPRECATION")

package io.github.wax911.library.model.body

@Deprecated(
    message = "Use co.anitrend.retrofit.graphql.model.body.GraphContainer instead",
    replaceWith = ReplaceWith(
        "GraphContainer",
        "co.anitrend.retrofit.graphql.model.body.GraphContainer"
    ),
    level = DeprecationLevel.WARNING
)
public typealias GraphContainer<T> = co.anitrend.retrofit.graphql.model.body.GraphContainer<T>
