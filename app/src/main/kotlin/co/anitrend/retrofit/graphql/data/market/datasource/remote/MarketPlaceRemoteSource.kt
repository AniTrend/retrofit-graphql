package co.anitrend.retrofit.graphql.data.market.datasource.remote

import co.anitrend.retrofit.graphql.data.api.common.EndpointType
import co.anitrend.retrofit.graphql.model.body.GraphContainer
import co.anitrend.retrofit.graphql.model.GraphQLRequest
import co.anitrend.retrofit.graphql.sample.generated.GetMarketPlaceAppsData
import co.anitrend.retrofit.graphql.sample.generated.GetMarketPlaceAppsVariables
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

internal interface MarketPlaceRemoteSource {

    @POST(EndpointType.BASE_ENDPOINT_PATH)
    suspend fun getMarketPlaceApps(
        @Body request: GraphQLRequest<GetMarketPlaceAppsVariables>
    ): Response<GraphContainer<GetMarketPlaceAppsData>>
}
