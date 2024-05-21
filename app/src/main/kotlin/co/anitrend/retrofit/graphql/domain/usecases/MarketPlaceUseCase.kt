package co.anitrend.retrofit.graphql.domain.usecases

import co.anitrend.arch.domain.common.IUseCase
import co.anitrend.arch.domain.state.UiState
import co.anitrend.retrofit.graphql.domain.repositories.MarketPlaceRepository

abstract class MarketPlaceUseCase<R: UiState<*>>(
    protected val repository: MarketPlaceRepository<R>
) : IUseCase {
    operator fun invoke() =
        repository.getMarketPlaceListings()
}