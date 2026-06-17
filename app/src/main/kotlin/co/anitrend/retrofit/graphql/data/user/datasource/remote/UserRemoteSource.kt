package co.anitrend.retrofit.graphql.data.user.datasource.remote

import co.anitrend.retrofit.graphql.data.api.common.EndpointType
import co.anitrend.retrofit.graphql.data.user.model.Viewer
import co.anitrend.retrofit.graphql.annotation.GraphQuery
import co.anitrend.retrofit.graphql.model.body.GraphContainer
import co.anitrend.retrofit.graphql.model.request.QueryContainerBuilder
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

internal interface UserRemoteSource {

    @POST(EndpointType.BASE_ENDPOINT_PATH)
    @GraphQuery("GetCurrentUser")
    suspend fun getCurrentUser(
        @Body builder: QueryContainerBuilder
    ): Response<GraphContainer<Viewer>>
}