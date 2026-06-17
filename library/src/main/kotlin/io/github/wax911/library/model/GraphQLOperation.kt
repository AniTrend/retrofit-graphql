@file:Suppress("DEPRECATION")

package io.github.wax911.library.model

@Deprecated(
    message = "Use co.anitrend.retrofit.graphql.model.GraphQLOperation instead",
    replaceWith = ReplaceWith(
        "GraphQLOperation",
        "co.anitrend.retrofit.graphql.model.GraphQLOperation"
    ),
    level = DeprecationLevel.WARNING
)
public typealias GraphQLOperation<TVariables> = co.anitrend.retrofit.graphql.model.GraphQLOperation<TVariables>
