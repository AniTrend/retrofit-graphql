package co.anitrend.retrofit.graphql.data.market.source

import co.anitrend.arch.extension.dispatchers.contract.ISupportDispatcher
import co.anitrend.arch.paging.legacy.FlowPagedListBuilder
import co.anitrend.arch.paging.legacy.util.PAGING_CONFIGURATION
import co.anitrend.arch.request.callback.RequestCallback
import co.anitrend.retrofit.graphql.data.arch.controller.SampleEnvelope
import co.anitrend.retrofit.graphql.data.arch.controller.strategy.ControllerStrategy
import co.anitrend.retrofit.graphql.data.arch.extensions.controller
import co.anitrend.retrofit.graphql.data.arch.extensions.mapBody
import co.anitrend.retrofit.graphql.data.market.converters.MarketPlaceEntityConverter
import co.anitrend.retrofit.graphql.data.market.datasource.local.MarketPlaceLocalSource
import co.anitrend.retrofit.graphql.data.market.datasource.remote.MarketPlaceRemoteSource
import co.anitrend.retrofit.graphql.data.market.entity.MarketPlaceEntity
import co.anitrend.retrofit.graphql.data.market.mapper.MarketPlaceResponseMapper
import co.anitrend.retrofit.graphql.data.market.model.query.MarketPlaceListingQuery
import co.anitrend.retrofit.graphql.data.market.source.contract.MarketPlaceSource
import co.anitrend.retrofit.graphql.model.GraphQLResponse
import co.anitrend.retrofit.graphql.sample.generated.GetMarketPlaceApps
import co.anitrend.retrofit.graphql.sample.generated.GetMarketPlaceAppsData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

internal class MarketPlaceSourceImpl(
    private val mapper: MarketPlaceResponseMapper,
    private val converter: MarketPlaceEntityConverter,
    private val remoteSource: MarketPlaceRemoteSource,
    private val localSource: MarketPlaceLocalSource,
    private val strategy: ControllerStrategy<List<MarketPlaceEntity>>,
    override val dispatcher: ISupportDispatcher
) : MarketPlaceSource() {

    override val observable = FlowPagedListBuilder(
        dataSourceFactory = localSource.findAllByFactory()
            .mapByPage { entities ->
                converter.convertFrom(entities)
            },
        config = PAGING_CONFIGURATION,
        initialLoadKey = null,
        boundaryCallback = this,
    ).buildFlow()

    /**
     * Invoked when a request to the network needs to happen
     */
    override suspend fun getMarketPlaceListing(
        requestCallback: RequestCallback,
        marketPlaceListingQuery: MarketPlaceListingQuery,
    ) {
        val deferred = async {
            remoteSource.getMarketPlaceApps(
                GetMarketPlaceApps.request(
                    first = marketPlaceListingQuery.first,
                    after = marketPlaceListingQuery.after,
                    before = marketPlaceListingQuery.before,
                )
            ).mapBody<GraphQLResponse<GetMarketPlaceAppsData>, SampleEnvelope<GetMarketPlaceAppsData>> {
                SampleEnvelope.Neutral(it)
            }
        }

        val controller =
            mapper.controller(strategy, dispatcher)

        controller(deferred, requestCallback)
    }

    /**
     * Clears data sources (databases, preferences, e.t.c)
     *
     * @param context Dispatcher context to run in
     */
    override suspend fun clearDataSource(context: CoroutineDispatcher) {
        withContext(dispatcher.io) {
            localSource.clear()
        }
    }
}
