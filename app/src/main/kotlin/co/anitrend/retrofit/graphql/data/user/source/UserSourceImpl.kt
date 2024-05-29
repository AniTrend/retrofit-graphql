package co.anitrend.retrofit.graphql.data.user.source

import co.anitrend.arch.extension.dispatchers.contract.ISupportDispatcher
import co.anitrend.arch.request.callback.RequestCallback
import co.anitrend.retrofit.graphql.data.arch.controller.strategy.ControllerStrategy
import co.anitrend.retrofit.graphql.data.arch.extensions.controller
import co.anitrend.retrofit.graphql.data.authentication.settings.IAuthenticationSettings
import co.anitrend.retrofit.graphql.data.user.converters.UserEntityConverter
import co.anitrend.retrofit.graphql.data.user.datasource.local.UserLocalSource
import co.anitrend.retrofit.graphql.data.user.datasource.remote.UserRemoteSource
import co.anitrend.retrofit.graphql.data.user.entity.UserEntity
import co.anitrend.retrofit.graphql.data.user.mapper.UserResponseMapper
import co.anitrend.retrofit.graphql.data.user.source.contract.UserSource
import io.github.wax911.library.model.request.QueryContainerBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class UserSourceImpl(
    private val settings: IAuthenticationSettings,
    private val mapper: UserResponseMapper,
    private val converter: UserEntityConverter,
    private val remoteSource: UserRemoteSource,
    private val localSource: UserLocalSource,
    private val strategy: ControllerStrategy<UserEntity>,
    override val dispatcher: ISupportDispatcher,
) : UserSource() {

    override val observable = flow {
        val authId = settings.authenticatedUserId.value
        val userFlowEntity = if (authId.isNotEmpty())
            localSource.getUserById(authId)
        else localSource.getDefaultUser()

        val userFlow = userFlowEntity.map { entity ->
            entity?.let { converter.convertFrom(it) }
        }
        emitAll(userFlow)
    }

    override suspend fun getCurrentUser(requestCallback: RequestCallback) {
        val authId = settings.authenticatedUserId.value
        // simulating some sort of cache refresh policy
        if (authId.isNotEmpty()) return
        val deferred = async {
            val queryBuilder = QueryContainerBuilder()
            remoteSource.getCurrentUser(queryBuilder)
        }

        val controller =
            mapper.controller(strategy, dispatcher)

        val result = controller(deferred, requestCallback)
        if (result != null)
            settings.authenticatedUserId.value = result.id
    }

    /**
     * Clears data sources (databases, preferences, e.t.c)
     *
     * @param context Dispatcher context to run in
     */
    override suspend fun clearDataSource(context: CoroutineDispatcher) {
        withContext(context) {
            settings.authenticatedUserId.value = IAuthenticationSettings.INVALID_USER_ID
            localSource.clear()
        }
    }
}