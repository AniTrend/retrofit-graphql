package co.anitrend.retrofit.graphql.data.market.source.contract

import androidx.paging.PagedList
import co.anitrend.arch.paging.legacy.source.SupportPagingDataSource
import co.anitrend.arch.request.callback.RequestCallback
import co.anitrend.arch.request.model.Request
import co.anitrend.retrofit.graphql.data.market.model.query.MarketPlaceListingQuery
import co.anitrend.retrofit.graphql.domain.entities.market.MarketPlaceListing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

internal abstract class MarketPlaceSource : SupportPagingDataSource<MarketPlaceListing>() {

    protected abstract val observable: Flow<PagedList<MarketPlaceListing>>

    protected abstract suspend fun getMarketPlaceListing(
        requestCallback: RequestCallback,
        marketPlaceListingQuery: MarketPlaceListingQuery,
    )

    operator fun invoke(): Flow<PagedList<MarketPlaceListing>> {
        return observable
    }

    /**
     * Called when the item at the end of the PagedList has been loaded, and access has
     * occurred within [Config.prefetchDistance] of it.
     *
     *
     * No more data will be appended to the PagedList after this item.
     *
     * @param itemAtEnd The first item of PagedList
     */
    override fun onItemAtEndLoaded(itemAtEnd: MarketPlaceListing) {
        super.onItemAtEndLoaded(itemAtEnd)
        scope.launch {
            val request = Request.Default(itemAtEnd.cursorId, Request.Type.AFTER)
            requestHelper.runIfNotRunning(request) { requestCallback ->
                val query = MarketPlaceListingQuery(
                    after = itemAtEnd.cursorId,
                    first = supportPagingHelper.pageSize
                )
                getMarketPlaceListing(requestCallback, query)
            }
        }
    }

    /**
     * Called when the item at the front of the PagedList has been loaded, and access has
     * occurred within [Config.prefetchDistance] of it.
     *
     *
     * No more data will be prepended to the PagedList before this item.
     *
     * @param itemAtFront The first item of PagedList
     */
    override fun onItemAtFrontLoaded(itemAtFront: MarketPlaceListing) {
        super.onItemAtFrontLoaded(itemAtFront)
        scope.launch {
            val request = Request.Default(itemAtFront.cursorId, Request.Type.BEFORE)
            requestHelper.runIfNotRunning(request) { requestCallback ->
                val query = MarketPlaceListingQuery(
                    before = itemAtFront.cursorId,
                    first = supportPagingHelper.pageSize
                )
                getMarketPlaceListing(requestCallback, query)
            }
        }
    }

    /**
     * Called when zero items are returned from an initial load of the PagedList's data source.
     */
    override fun onZeroItemsLoaded() {
        super.onZeroItemsLoaded()
        scope.launch {
            val request = Request.Default("initial", Request.Type.INITIAL)
            requestHelper.runIfNotRunning(request) { requestCallback ->
                val query = MarketPlaceListingQuery(
                    first = supportPagingHelper.pageSize
                )
                getMarketPlaceListing(requestCallback, query)
            }
        }
    }
}