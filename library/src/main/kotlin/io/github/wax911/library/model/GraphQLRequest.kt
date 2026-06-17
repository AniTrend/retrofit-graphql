@file:Suppress("DEPRECATION")

package io.github.wax911.library.model

@Deprecated(
    message = "Use co.anitrend.retrofit.graphql.model.GraphQLRequest instead",
    replaceWith = ReplaceWith(
        "GraphQLRequest",
        "co.anitrend.retrofit.graphql.model.GraphQLRequest"
    ),
    level = DeprecationLevel.WARNING
)
public typealias GraphQLRequest<TVariables> = co.anitrend.retrofit.graphql.model.GraphQLRequest<TVariables>
