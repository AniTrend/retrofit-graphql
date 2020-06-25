package co.anitrend.retrofit.graphql.data.market.datasource.remote

import co.anitrend.retrofit.graphql.data.api.common.EndpointType
import co.anitrend.retrofit.graphql.data.market.model.MarketPlaceListings
import io.github.wax911.library.annotation.GraphQuery
import io.github.wax911.library.model.body.GraphContainer
import io.github.wax911.library.model.request.QueryContainerBuilder
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

internal interface MarketPlaceRemoteSource {

    @POST(EndpointType.BASE_ENDPOINT_PATH)
    @GraphQuery("GetMarketPlaceApps")
    suspend fun getMarketPlaceApps(
        @Body builder: QueryContainerBuilder
    ): Response<GraphContainer<MarketPlaceListings>>
}