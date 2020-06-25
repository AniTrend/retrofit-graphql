package co.anitrend.retrofit.graphql.data.arch.controller.policy

import androidx.lifecycle.MutableLiveData
import androidx.paging.PagingRequestHelper
import co.anitrend.arch.domain.entities.NetworkState
import co.anitrend.arch.extension.network.SupportConnectivity
import co.anitrend.retrofit.graphql.data.arch.controller.strategy.ControllerStrategy
import timber.log.Timber

/**
 * Runs connectivity check before prior to execution
 */
internal class OnlineControllerPolicy<D> private constructor(
    private val connectivity: SupportConnectivity
) : ControllerStrategy<D>() {

    /**
     * Execute a paging task under an implementation strategy
     *
     * @param block what will be executed
     * @param pagingRequestHelper paging event emitter
     */
    override suspend fun invoke(
        block: suspend () -> Unit,
        pagingRequestHelper: PagingRequestHelper.Request.Callback
    ) {
        if (connectivity.isConnected) {
            runCatching {
                block()
                pagingRequestHelper.recordSuccess()
            }.exceptionOrNull()?.also { e ->
                e.printStackTrace()
                Timber.tag(moduleTag).e(e)
                pagingRequestHelper.recordFailure(e)
            }
        }
        else {
            pagingRequestHelper.recordFailure(
                Throwable("Please check your internet connection")
            )
        }
    }

    /**
     * Execute a task under an implementation strategy
     *
     * @param block what will be executed
     * @param networkState network state event emitter
     */
    override suspend fun invoke(
        block: suspend () -> D?,
        networkState: MutableLiveData<NetworkState>
    ): D? {
        if (connectivity.isConnected) {
            return runCatching{
                networkState.postValue(NetworkState.Loading)
                val result = block()
                networkState.postValue(NetworkState.Success)
                result
            }.getOrElse {
                Timber.tag(moduleTag).e(it)
                networkState.postValue(
                    NetworkState.Error(
                        heading = it.cause?.message ?: "Unexpected error encountered \uD83E\uDD2D",
                        message = it.message
                    )
                )
                null
            }
        } else {
            networkState.postValue(
                NetworkState.Error(
                    heading = "No internet connection detected \uD83E\uDD2D",
                    message = "Please check your internet connection"
                )
            )
        }
        return null
    }

    companion object {
        fun <T> create(connectivity: SupportConnectivity) =
            OnlineControllerPolicy<T>(
                connectivity
            )
    }
}