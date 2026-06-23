package co.anitrend.retrofit.graphql.data.bucket.datasource.remote

import co.anitrend.retrofit.graphql.data.api.common.EndpointType
import co.anitrend.retrofit.graphql.data.bucket.model.StorageBucket
import co.anitrend.retrofit.graphql.data.bucket.model.upload.UploadResult
import co.anitrend.retrofit.graphql.model.body.GraphContainer
import co.anitrend.retrofit.graphql.model.EmptyGraphQLVariables
import co.anitrend.retrofit.graphql.model.GraphQLRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

internal interface BucketRemoteSource {

    @POST(EndpointType.BASE_ENDPOINT_PATH)
    suspend fun getStorageBucketFiles(
        @Body request: GraphQLRequest<EmptyGraphQLVariables>
    ): Response<GraphContainer<StorageBucket>>

    @POST(EndpointType.BASE_ENDPOINT_PATH)
    suspend fun uploadToStorageBucket(
        @Body body: MultipartBody
    ): Response<GraphContainer<UploadResult>>
}
