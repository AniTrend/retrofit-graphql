package co.anitrend.retrofit.graphql.data.api.provider.extensions

import co.anitrend.retrofit.graphql.data.api.common.EndpointType
import co.anitrend.retrofit.graphql.data.api.provider.RetrofitProvider
import org.koin.core.scope.Scope

/**
 * Facade for supplying retrofit interface types
 */
internal inline fun <reified T> Scope.api(endpointType: EndpointType): T =
    RetrofitProvider.provideRetrofit(endpointType, this).create(T::class.java)