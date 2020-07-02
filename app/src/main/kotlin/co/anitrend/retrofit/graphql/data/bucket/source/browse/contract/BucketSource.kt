package co.anitrend.retrofit.graphql.data.bucket.source.browse.contract

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import co.anitrend.arch.data.source.coroutine.SupportCoroutineDataSource
import co.anitrend.arch.extension.dispatchers.SupportDispatchers
import co.anitrend.retrofit.graphql.domain.entities.bucket.BucketFile
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull

internal abstract class BucketSource(
    dispatchers: SupportDispatchers
) : SupportCoroutineDataSource(dispatchers) {

    protected abstract val observable: StateFlow<List<BucketFile>?>

    protected open suspend fun getStorageBucketFiles() {
        retry = { getStorageBucketFiles() }
    }

    operator fun invoke(): LiveData<List<BucketFile>> =
        liveData {
            getStorageBucketFiles()
            val bucketFlow = observable.mapNotNull { it }
            emitSource(bucketFlow.asLiveData())
        }
}