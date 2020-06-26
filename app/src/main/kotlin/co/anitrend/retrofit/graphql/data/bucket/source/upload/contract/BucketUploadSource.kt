package co.anitrend.retrofit.graphql.data.bucket.source.upload.contract

import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import co.anitrend.arch.data.source.coroutine.SupportCoroutineDataSource
import co.anitrend.arch.extension.dispatchers.SupportDispatchers
import co.anitrend.retrofit.graphql.domain.entities.bucket.BucketFile
import co.anitrend.retrofit.graphql.domain.models.common.IGraphQuery
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull

internal abstract class BucketUploadSource(
    dispatchers: SupportDispatchers
) : SupportCoroutineDataSource(dispatchers) {

    protected abstract val observable: StateFlow<BucketFile?>

    protected open suspend fun uploadToStorageBucket(mutation: IGraphQuery) {
        retry = { uploadToStorageBucket(mutation) }
    }

    operator fun invoke(mutation: IGraphQuery) =
        liveData<BucketFile> {
            uploadToStorageBucket(mutation)
            val bucketFlow = observable.mapNotNull { it }
            emitSource(bucketFlow.asLiveData())
        }
}