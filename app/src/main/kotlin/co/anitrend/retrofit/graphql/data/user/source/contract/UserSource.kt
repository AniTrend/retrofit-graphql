package co.anitrend.retrofit.graphql.data.user.source.contract

import co.anitrend.arch.data.source.core.SupportCoreDataSource
import co.anitrend.arch.request.callback.RequestCallback
import co.anitrend.arch.request.model.Request
import co.anitrend.retrofit.graphql.domain.entities.user.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

internal abstract class UserSource : SupportCoreDataSource() {

    protected abstract val observable: Flow<User?>

    protected abstract suspend fun getCurrentUser(
        requestCallback: RequestCallback
    )

    operator fun invoke(): Flow<User> {
        scope.launch {
            requestHelper.runIfNotRunning(
                request = Request.Default("getCurrentUser", Request.Type.INITIAL),
                handleCallback = ::getCurrentUser
            )
        }
        return observable.mapNotNull { it }
    }
}