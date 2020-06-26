package co.anitrend.retrofit.graphql.data.arch.controller.strategy

import androidx.lifecycle.MutableLiveData
import androidx.paging.PagingRequestHelper
import co.anitrend.arch.domain.entities.NetworkState

internal abstract class ControllerStrategy<D> {

    protected val moduleTag = javaClass.simpleName

    /**
     * Execute a paging task under an implementation strategy
     *
     * @param block what will be executed
     * @param pagingRequestHelper paging event emitter
     */
    internal abstract suspend operator fun invoke(
        block: suspend () -> Unit,
        pagingRequestHelper: PagingRequestHelper.Request.Callback
    )

    /**
     * Execute a task under an implementation strategy
     *
     * @param block what will be executed
     * @param networkState network state event emitter
     */
    internal abstract suspend operator fun invoke(
        block: suspend () -> D?,
        networkState: MutableLiveData<NetworkState>
    ): D?
}