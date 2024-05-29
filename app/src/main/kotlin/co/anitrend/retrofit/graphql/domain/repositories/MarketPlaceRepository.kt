package co.anitrend.retrofit.graphql.domain.repositories

import co.anitrend.arch.domain.state.UiState

interface MarketPlaceRepository<D: UiState<*>> {
    fun getMarketPlaceListings(): D
}