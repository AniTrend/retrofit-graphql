package co.anitrend.retrofit.graphql.data.arch.controller.policy

import co.anitrend.arch.domain.entities.RequestError
import co.anitrend.arch.extension.network.SupportConnectivity
import co.anitrend.arch.request.callback.RequestCallback
import co.anitrend.retrofit.graphql.data.arch.controller.strategy.ControllerStrategy
import timber.log.Timber

/**
 * Runs connectivity check before prior to execution
 */
internal class OnlineControllerPolicy<D> private constructor(
    private val connectivity: SupportConnectivity
) : ControllerStrategy<D>() {


    /**
     * Execute a task under an implementation strategy
     *
     * @param callback event emitter
     * @param block what will be executed
     */
    override suspend fun invoke(
        callback: RequestCallback,
        block: suspend () -> D?
    ): D? {
        runCatching {
            if (connectivity.isConnected)
                block()
            else
                throw RequestError(
                    "No internet connection",
                    "Make sure you have an active internet connection"
                )
        }.onSuccess { result ->
            callback.recordSuccess()
            return result
        }.onFailure { exception ->
            Timber.e(exception)
            when (exception) {
                is RequestError -> callback.recordFailure(exception)
                else -> callback.recordFailure(RequestError(exception))
            }
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