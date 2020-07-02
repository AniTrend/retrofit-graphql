package co.anitrend.retrofit.graphql.data.market.usecase

import androidx.paging.PagedList
import co.anitrend.arch.data.model.UserInterfaceState
import co.anitrend.arch.data.repository.contract.ISupportRepository
import co.anitrend.retrofit.graphql.data.market.repository.MarketPlaceRepositoryContract
import co.anitrend.retrofit.graphql.domain.entities.market.MarketPlaceListing
import co.anitrend.retrofit.graphql.domain.usecases.MarketPlaceUseCase

internal class MarketPlaceUseCaseImpl(
    repository: MarketPlaceRepositoryContract
) : MarketPlaceUseCaseContract(repository) {

    /**
     * Informs underlying repositories or related components running background operations to stop
     */
    override fun onCleared() {
        repository as ISupportRepository
        repository.onCleared()
    }
}

typealias MarketPlaceUseCaseContract = MarketPlaceUseCase<UserInterfaceState<PagedList<MarketPlaceListing>>>