@file:Suppress("DEPRECATION")

package io.github.wax911.library.annotation.processor

@Deprecated(
    message = "Use co.anitrend.retrofit.graphql.annotation.processor.GraphProcessor instead",
    replaceWith = ReplaceWith(
        "GraphProcessor",
        "co.anitrend.retrofit.graphql.annotation.processor.GraphProcessor"
    ),
    level = DeprecationLevel.WARNING
)
public typealias GraphProcessor = co.anitrend.retrofit.graphql.annotation.processor.GraphProcessor
