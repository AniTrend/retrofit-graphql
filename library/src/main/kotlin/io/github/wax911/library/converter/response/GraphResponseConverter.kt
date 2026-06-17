@file:Suppress("DEPRECATION")

package io.github.wax911.library.converter.response

@Deprecated(
    message = "Use co.anitrend.retrofit.graphql.converter.response.GraphResponseConverter instead",
    replaceWith = ReplaceWith(
        "GraphResponseConverter",
        "co.anitrend.retrofit.graphql.converter.response.GraphResponseConverter"
    ),
    level = DeprecationLevel.WARNING
)
public typealias GraphResponseConverter<T> = co.anitrend.retrofit.graphql.converter.response.GraphResponseConverter<T>
