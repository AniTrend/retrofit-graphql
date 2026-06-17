@file:Suppress("DEPRECATION")

package io.github.wax911.library.annotation.processor.plugin.contract

@Deprecated(
    message = "Use co.anitrend.retrofit.graphql.annotation.processor.plugin.contract.AbstractDiscoveryPlugin instead",
    replaceWith = ReplaceWith(
        "AbstractDiscoveryPlugin",
        "co.anitrend.retrofit.graphql.annotation.processor.plugin.contract.AbstractDiscoveryPlugin"
    ),
    level = DeprecationLevel.WARNING
)
public typealias AbstractDiscoveryPlugin<S> = co.anitrend.retrofit.graphql.annotation.processor.plugin.contract.AbstractDiscoveryPlugin<S>
