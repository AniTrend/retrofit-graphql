package co.anitrend.retrofit.graphql.data.arch.common

import androidx.paging.PagedList
import androidx.paging.PagingRequestHelper
import co.anitrend.arch.data.source.paging.SupportPagingDataSource
import co.anitrend.arch.extension.dispatchers.SupportDispatchers
import kotlinx.coroutines.launch

internal abstract class SamplePagedSource<T>(
    supportDispatchers: SupportDispatchers
) : SupportPagingDataSource<T>(supportDispatchers) {

    /**
     * Invoked when a request to the network needs to happen
     */
    abstract suspend operator fun invoke(
        callback: PagingRequestHelper.Request.Callback,
        requestType: PagingRequestHelper.RequestType,
        model: T?
    )

    /**
     * Called when the item at the end of the PagedList has been loaded, and access has
     * occurred within [PagedList.Config.prefetchDistance] of it.
     *
     * No more data will be appended to the [PagedList] after this item.
     *
     * @param itemAtEnd The first item of [PagedList]
     */
    override fun onItemAtEndLoaded(itemAtEnd: T) {
        val requestType = PagingRequestHelper.RequestType.AFTER
        pagingRequestHelper.runIfNotRunning(
            requestType
        ) { pagingRequestCallback ->
            launch {
                invoke(
                    pagingRequestCallback,
                    requestType,
                    itemAtEnd
                )
            }
        }
    }

    /**
     * Called when the item at the front of the PagedList has been loaded, and access has
     * occurred within [PagedList.Config.prefetchDistance] of it.
     *
     * No more data will be prepended to the PagedList before this item.
     *
     * @param itemAtFront The first item of PagedList
     */
    override fun onItemAtFrontLoaded(itemAtFront: T) {
        val requestType = PagingRequestHelper.RequestType.BEFORE
        pagingRequestHelper.runIfNotRunning(
            requestType
        ) { pagingRequestCallback ->
            launch {
                invoke(
                    pagingRequestCallback,
                    requestType,
                    itemAtFront
                )
            }
        }
    }

    /**
     * Called when zero items are returned from an initial load of the PagedList's data source.
     */
    override fun onZeroItemsLoaded() {
        val requestType = PagingRequestHelper.RequestType.INITIAL
        pagingRequestHelper.runIfNotRunning(
            PagingRequestHelper.RequestType.INITIAL
        ) { pagingRequestCallback ->
            launch {
                invoke(
                    pagingRequestCallback,
                    PagingRequestHelper.RequestType.INITIAL,
                    null
                )
            }
        }
    }
}