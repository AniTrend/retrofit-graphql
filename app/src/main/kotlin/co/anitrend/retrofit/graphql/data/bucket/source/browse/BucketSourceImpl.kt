package co.anitrend.retrofit.graphql.data.bucket.source.browse

import co.anitrend.arch.extension.dispatchers.contract.ISupportDispatcher
import co.anitrend.arch.request.callback.RequestCallback
import co.anitrend.retrofit.graphql.data.arch.controller.strategy.ControllerStrategy
import co.anitrend.retrofit.graphql.data.arch.extensions.controller
import co.anitrend.retrofit.graphql.data.bucket.datasource.remote.BucketRemoteSource
import co.anitrend.retrofit.graphql.data.bucket.mapper.BucketResponseMapper
import co.anitrend.retrofit.graphql.data.bucket.source.browse.contract.BucketSource
import co.anitrend.retrofit.graphql.domain.entities.bucket.BucketFile
import io.github.wax911.library.model.request.QueryContainerBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow

internal class BucketSourceImpl(
    private val mapper: BucketResponseMapper,
    private val remoteSource: BucketRemoteSource,
    private val strategy: ControllerStrategy<List<BucketFile>>,
    override val dispatcher: ISupportDispatcher,
) : BucketSource() {

    override val observable =
        MutableStateFlow<List<BucketFile>?>(null)

    override suspend fun getStorageBucketFiles(requestCallback: RequestCallback) {
        val deferred = async {
            val queryBuilder = QueryContainerBuilder()
            remoteSource.getStorageBucketFiles(
                queryBuilder
            )
        }

        val controller =
            mapper.controller(strategy, dispatcher)

        val result = controller(deferred, requestCallback)
        observable.value = result
    }

    /**
     * Clears data sources (databases, preferences, e.t.c)
     *
     * @param context Dispatcher context to run in
     */
    override suspend fun clearDataSource(context: CoroutineDispatcher) {
        observable.value = null
    }
}