package co.anitrend.retrofit.graphql.data.bucket.source.upload.contract

import co.anitrend.arch.data.source.core.SupportCoreDataSource
import co.anitrend.arch.request.callback.RequestCallback
import co.anitrend.arch.request.model.Request
import co.anitrend.retrofit.graphql.domain.entities.bucket.BucketFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

internal abstract class BucketUploadSource: SupportCoreDataSource() {

    protected abstract val observable: StateFlow<BucketFile?>

    protected abstract suspend fun uploadToStorageBucket(
        path: String,
        requestCallback: RequestCallback
    )

    operator fun invoke(path: String): Flow<BucketFile> {
        scope.launch {
            requestHelper.runIfNotRunning(
                Request.Default("uploadToStorageBucket", Request.Type.INITIAL)
            ) {
                uploadToStorageBucket(path, it)
            }
        }
        return observable.mapNotNull { it }
    }
}
