package co.anitrend.retrofit.graphql.domain.usecases

import co.anitrend.arch.domain.common.IUseCase
import co.anitrend.arch.domain.common.IUserInterfaceState
import co.anitrend.retrofit.graphql.domain.repositories.MarketPlaceRepository

abstract class MarketPlaceUseCase<R: IUserInterfaceState<*>>(
    protected val repository: MarketPlaceRepository<R>
) : IUseCase {
    operator fun invoke() =
        repository.getMarketPlaceListings()
}