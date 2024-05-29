package co.anitrend.retrofit.graphql.data.market.repository

import androidx.paging.PagedList
import co.anitrend.arch.data.repository.SupportRepository
import co.anitrend.arch.data.state.DataState
import co.anitrend.arch.data.state.DataState.Companion.create
import co.anitrend.retrofit.graphql.data.market.source.contract.MarketPlaceSource
import co.anitrend.retrofit.graphql.domain.entities.market.MarketPlaceListing
import co.anitrend.retrofit.graphql.domain.repositories.MarketPlaceRepository

internal class MarketPlaceRepositoryImpl(
    private val source: MarketPlaceSource
) : SupportRepository(source), MarketPlaceRepositoryContract {

    override fun getMarketPlaceListings() =
        source.create(
            model = source()
        )
}

typealias MarketPlaceRepositoryContract = MarketPlaceRepository<DataState<PagedList<MarketPlaceListing>>>