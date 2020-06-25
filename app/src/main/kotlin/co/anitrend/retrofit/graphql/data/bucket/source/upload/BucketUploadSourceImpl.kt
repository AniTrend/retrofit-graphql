package co.anitrend.retrofit.graphql.data.bucket.source.upload

import co.anitrend.arch.extension.dispatchers.SupportDispatchers
import co.anitrend.retrofit.graphql.data.arch.controller.strategy.ControllerStrategy
import co.anitrend.retrofit.graphql.data.arch.extensions.controller
import co.anitrend.retrofit.graphql.data.bucket.datasource.remote.BucketRemoteSource
import co.anitrend.retrofit.graphql.data.bucket.mapper.UploadResponseMapper
import co.anitrend.retrofit.graphql.data.bucket.source.upload.contract.BucketUploadSource
import co.anitrend.retrofit.graphql.domain.entities.bucket.BucketFile
import co.anitrend.retrofit.graphql.domain.models.common.IGraphQuery
import io.github.wax911.library.model.request.QueryContainerBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow

internal class BucketUploadSourceImpl(
    private val mapper: UploadResponseMapper,
    private val remoteSource: BucketRemoteSource,
    private val strategy: ControllerStrategy<BucketFile>,
    dispatchers: SupportDispatchers
) : BucketUploadSource(dispatchers) {

    override val observable =
        MutableStateFlow<BucketFile?>(null)

    override suspend fun uploadToStorageBucket(mutation: IGraphQuery) {
        super.uploadToStorageBucket(mutation)
        val deferred = async {
            val queryBuilder = QueryContainerBuilder()
                .putVariables(mutation.toMap())
            remoteSource.uploadToStorageBucket(queryBuilder)
        }

        val controller =
            mapper.controller(strategy, dispatchers)

        val result = controller(deferred, networkState)
        observable.value = result
    }

    /**
     * Clears data sources (databases, preferences, e.t.c)
     */
    override suspend fun clearDataSource() {
        observable.value = null
    }
}