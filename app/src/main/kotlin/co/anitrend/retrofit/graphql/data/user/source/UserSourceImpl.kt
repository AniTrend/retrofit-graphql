package co.anitrend.retrofit.graphql.data.user.source

import co.anitrend.arch.extension.dispatchers.SupportDispatchers
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
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal class UserSourceImpl(
    private val settings: IAuthenticationSettings,
    private val mapper: UserResponseMapper,
    private val converter: UserEntityConverter,
    private val remoteSource: UserRemoteSource,
    private val localSource: UserLocalSource,
    private val strategy: ControllerStrategy<UserEntity>,
    dispatchers: SupportDispatchers
) : UserSource(dispatchers) {

    override val observable = flow {
        val userFlowEntity = if (
            settings.authenticatedUserId.isNotEmpty()
        ) localSource.getUserById(settings.authenticatedUserId)
        else localSource.getDefaultUser()

        val userFlow = userFlowEntity.map { entity ->
            entity?.let { converter.convertFrom(it) }
        }
        emitAll(userFlow)
    }

    override suspend fun getCurrentUser() {
        super.getCurrentUser()
        // simulating some sort of cache refresh policy
        if (settings.authenticatedUserId.isNotEmpty()) return
        val deferred = async {
            val queryBuilder = QueryContainerBuilder()
            remoteSource.getCurrentUser(queryBuilder)
        }

        val controller =
            mapper.controller(strategy, dispatchers)

        val result = controller(deferred, networkState)
        if (result != null)
            settings.authenticatedUserId = result.id
    }

    /**
     * Clears data sources (databases, preferences, e.t.c)
     */
    override suspend fun clearDataSource() {
        settings.authenticatedUserId = IAuthenticationSettings.INVALID_USER_ID
        localSource.clear()
    }
}