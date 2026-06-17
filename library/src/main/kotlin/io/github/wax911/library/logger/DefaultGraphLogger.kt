@file:Suppress("DEPRECATION")

package io.github.wax911.library.logger

@Deprecated(
    message = "Use co.anitrend.retrofit.graphql.logger.DefaultGraphLogger instead",
    replaceWith = ReplaceWith(
        "DefaultGraphLogger",
        "co.anitrend.retrofit.graphql.logger.DefaultGraphLogger"
    ),
    level = DeprecationLevel.WARNING
)
public typealias DefaultGraphLogger = co.anitrend.retrofit.graphql.logger.DefaultGraphLogger
