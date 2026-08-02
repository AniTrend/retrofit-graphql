package co.anitrend.retrofit.graphql.data.user.datasource.remote

import co.anitrend.retrofit.graphql.data.api.common.EndpointType
import co.anitrend.retrofit.graphql.model.EmptyGraphQLVariables
import co.anitrend.retrofit.graphql.model.GraphQLResponse
import co.anitrend.retrofit.graphql.model.request.GraphQLOperationRequest
import co.anitrend.retrofit.graphql.sample.generated.GetCurrentUserData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

internal interface UserRemoteSource {

    @POST(EndpointType.BASE_ENDPOINT_PATH)
    suspend fun getCurrentUser(
        @Body request: GraphQLOperationRequest<EmptyGraphQLVariables>
    ): Response<GraphQLResponse<GetCurrentUserData>>
}
