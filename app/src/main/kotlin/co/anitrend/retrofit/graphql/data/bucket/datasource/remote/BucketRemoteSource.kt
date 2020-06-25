package co.anitrend.retrofit.graphql.data.bucket.datasource.remote

import co.anitrend.retrofit.graphql.data.api.common.EndpointType
import co.anitrend.retrofit.graphql.data.bucket.model.StorageBucket
import co.anitrend.retrofit.graphql.data.bucket.model.upload.UploadResult
import io.github.wax911.library.annotation.GraphQuery
import io.github.wax911.library.model.body.GraphContainer
import io.github.wax911.library.model.request.QueryContainerBuilder
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

internal interface BucketRemoteSource {

    @POST(EndpointType.BASE_ENDPOINT_PATH)
    @GraphQuery("StorageBucketFiles")
    suspend fun getStorageBucketFiles(
        @Body builder: QueryContainerBuilder
    ): Response<GraphContainer<StorageBucket>>

    @POST(EndpointType.BASE_ENDPOINT_PATH)
    @GraphQuery("UploadToStorageBucket")
    suspend fun uploadToStorageBucket(
        @Body builder: QueryContainerBuilder
    ): Response<GraphContainer<UploadResult>>
}