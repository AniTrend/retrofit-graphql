package co.anitrend.retrofit.graphql.data.market.source

import androidx.lifecycle.liveData
import androidx.paging.PagedList
import androidx.paging.PagingRequestHelper
import androidx.paging.toLiveData
import co.anitrend.arch.data.util.SupportDataKeyStore
import co.anitrend.arch.extension.dispatchers.SupportDispatchers
import co.anitrend.retrofit.graphql.data.arch.controller.strategy.ControllerStrategy
import co.anitrend.retrofit.graphql.data.arch.extensions.controller
import co.anitrend.retrofit.graphql.data.market.converters.MarketPlaceEntityConverter
import co.anitrend.retrofit.graphql.data.market.datasource.local.MarketPlaceLocalSource
import co.anitrend.retrofit.graphql.data.market.datasource.remote.MarketPlaceRemoteSource
import co.anitrend.retrofit.graphql.data.market.entity.MarketPlaceEntity
import co.anitrend.retrofit.graphql.data.market.mapper.MarketPlaceResponseMapper
import co.anitrend.retrofit.graphql.data.market.model.query.MarketPlaceListingQuery
import co.anitrend.retrofit.graphql.data.market.source.contract.MarketPlaceSource
import co.anitrend.retrofit.graphql.domain.entities.market.MarketPlaceListing
import io.github.wax911.library.model.request.QueryContainerBuilder
import kotlinx.coroutines.async

internal class MarketPlaceSourceImpl(
    private val mapper: MarketPlaceResponseMapper,
    private val converter: MarketPlaceEntityConverter,
    private val remoteSource: MarketPlaceRemoteSource,
    private val localSource: MarketPlaceLocalSource,
    private val strategy: ControllerStrategy<List<MarketPlaceEntity>>,
    dispatchers: SupportDispatchers
) : MarketPlaceSource(dispatchers) {

    private fun buildMarketPlaceListingQuery(
        requestType: PagingRequestHelper.RequestType,
        model: MarketPlaceListing?
    ): MarketPlaceListingQuery {
        val query = MarketPlaceListingQuery(first = supportPagingHelper.pageSize)
        when (requestType) {
            PagingRequestHelper.RequestType.BEFORE ->
                query.before = model?.cursorId
            PagingRequestHelper.RequestType.AFTER ->
                query.after = model?.cursorId
            PagingRequestHelper.RequestType.INITIAL -> {}
        }
        return query
    }

    override val observable = liveData {
        val factory = localSource.findAllByFactory()
        val pagingSource = factory.mapByPage { entities ->
            converter.convertFrom(entities)
        }

        val callback: PagedList.BoundaryCallback<MarketPlaceListing> = this@MarketPlaceSourceImpl

        emitSource(
            pagingSource.toLiveData(
                config = SupportDataKeyStore.PAGING_CONFIGURATION,
                boundaryCallback = callback
            )
        )
    }

    /**
     * Invoked when a request to the network needs to happen
     */
    override suspend fun invoke(
        callback: PagingRequestHelper.Request.Callback,
        requestType: PagingRequestHelper.RequestType,
        model: MarketPlaceListing?
    ) {
        val marketPlaceQuery = buildMarketPlaceListingQuery(requestType, model)
        val deferred = async {
            val queryBuilder = QueryContainerBuilder()
                .putVariables(marketPlaceQuery.toMap())
            remoteSource.getMarketPlaceApps(queryBuilder)
        }

        val controller =
            mapper.controller(strategy, dispatchers)

        controller(deferred, callback)
    }

    /**
     * Clears data sources (databases, preferences, e.t.c)
     */
    override suspend fun clearDataSource() {
        localSource.clear()
    }
}