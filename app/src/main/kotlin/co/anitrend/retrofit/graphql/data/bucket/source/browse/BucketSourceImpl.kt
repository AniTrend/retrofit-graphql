package co.anitrend.retrofit.graphql.data.bucket.source.browse

import co.anitrend.arch.extension.dispatchers.SupportDispatchers
import co.anitrend.retrofit.graphql.data.arch.controller.strategy.ControllerStrategy
import co.anitrend.retrofit.graphql.data.arch.extensions.controller
import co.anitrend.retrofit.graphql.data.bucket.datasource.remote.BucketRemoteSource
import co.anitrend.retrofit.graphql.data.bucket.mapper.BucketResponseMapper
import co.anitrend.retrofit.graphql.data.bucket.source.browse.contract.BucketSource
import co.anitrend.retrofit.graphql.domain.entities.bucket.BucketFile
import io.github.wax911.library.model.request.QueryContainerBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow

internal class BucketSourceImpl(
    private val mapper: BucketResponseMapper,
    private val remoteSource: BucketRemoteSource,
    private val strategy: ControllerStrategy<List<BucketFile>>,
    dispatchers: SupportDispatchers
) : BucketSource(dispatchers) {

    override val observable =
        MutableStateFlow<List<BucketFile>?>(null)

    override suspend fun getStorageBucketFiles() {
        super.getStorageBucketFiles()
        val deferred = async {
            val queryBuilder = QueryContainerBuilder()
            remoteSource.getStorageBucketFiles(
                queryBuilder
            )
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