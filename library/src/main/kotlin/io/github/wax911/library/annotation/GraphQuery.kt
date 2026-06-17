@file:Suppress("DEPRECATION")

package io.github.wax911.library.annotation

@Deprecated(
    message = "Use co.anitrend.retrofit.graphql.annotation.GraphQuery instead",
    replaceWith = ReplaceWith(
        "GraphQuery",
        "co.anitrend.retrofit.graphql.annotation.GraphQuery"
    ),
    level = DeprecationLevel.WARNING
)
public typealias GraphQuery = co.anitrend.retrofit.graphql.annotation.GraphQuery
