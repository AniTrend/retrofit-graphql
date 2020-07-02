package co.anitrend.retrofit.graphql.data.user.source.contract

import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import co.anitrend.arch.data.source.coroutine.SupportCoroutineDataSource
import co.anitrend.arch.extension.dispatchers.SupportDispatchers
import co.anitrend.retrofit.graphql.domain.entities.user.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

internal abstract class UserSource(
    dispatchers: SupportDispatchers
) : SupportCoroutineDataSource(dispatchers) {

    protected abstract val observable: Flow<User?>

    protected open suspend fun getCurrentUser() {
        retry = { getCurrentUser() }
    }

    operator fun invoke() = liveData<User> {
        getCurrentUser()
        val userFlow = observable.mapNotNull { it }
        emitSource(userFlow.asLiveData())
    }
}