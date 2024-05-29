package co.anitrend.retrofit.graphql.data.bucket.source.browse.contract

import co.anitrend.arch.data.source.core.SupportCoreDataSource
import co.anitrend.arch.request.callback.RequestCallback
import co.anitrend.arch.request.model.Request
import co.anitrend.retrofit.graphql.domain.entities.bucket.BucketFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

internal abstract class BucketSource : SupportCoreDataSource() {

    protected abstract val observable: StateFlow<List<BucketFile>?>

    protected abstract suspend fun getStorageBucketFiles(requestCallback: RequestCallback)

    operator fun invoke(): Flow<List<BucketFile>> {
        scope.launch {
            requestHelper.runIfNotRunning(
                Request.Default("getStorageBucketFiles", Request.Type.INITIAL)
            ) {
                getStorageBucketFiles(it)
            }
        }
        return observable.mapNotNull { it }
    }
}