package co.anitrend.retrofit.graphql.sample.view.content.market.viewmodel

import androidx.lifecycle.ViewModel
import co.anitrend.retrofit.graphql.data.market.usecase.MarketPlaceUseCaseContract
import co.anitrend.retrofit.graphql.sample.view.content.market.viewmodel.state.MarketPlaceState

class MarketPlaceViewModel(
    private val useCase: MarketPlaceUseCaseContract
) : ViewModel() {

    val state = MarketPlaceState(useCase)

    /**
     * This method will be called when this ViewModel is no longer used and will be destroyed.
     *
     * It is useful when ViewModel observes some data and you need to clear this subscription to
     * prevent a leak of this ViewModel.
     */
    override fun onCleared() {
        useCase.onCleared()
        super.onCleared()
    }
}