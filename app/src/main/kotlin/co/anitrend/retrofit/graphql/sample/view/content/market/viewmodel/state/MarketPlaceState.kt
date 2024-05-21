package co.anitrend.retrofit.graphql.sample.view.content.market.viewmodel.state

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.paging.PagedList
import co.anitrend.arch.core.model.ISupportViewModelState
import co.anitrend.arch.data.model.UserInterfaceState
import co.anitrend.retrofit.graphql.data.market.usecase.MarketPlaceUseCaseContract
import co.anitrend.retrofit.graphql.domain.entities.market.MarketPlaceListing

data class MarketPlaceState(
    private val useCase: MarketPlaceUseCaseContract
) : ISupportViewModelState<PagedList<MarketPlaceListing>> {

    private val useCaseResult = MutableLiveData<UserInterfaceState<PagedList<MarketPlaceListing>>>()

    override val model =
        useCaseResult.switchMap() { it.model }
    override val networkState =
        useCaseResult.switchMap() { it.networkState }
    override val refreshState =
        useCaseResult.switchMap() { it.refreshState }

    operator fun invoke() {
        val result = useCase()
        useCaseResult.postValue(result)
    }

    /**
     * Called upon [androidx.lifecycle.ViewModel.onCleared] and should optionally
     * call cancellation of any ongoing jobs.
     *
     * If your use case source is of type [co.anitrend.arch.domain.common.IUseCase]
     * then you could optionally call [co.anitrend.arch.domain.common.IUseCase.onCleared] here
     */
    override fun onCleared() {
        useCase.onCleared()
    }

    /**
     * Triggers use case to perform refresh operation
     */
    override fun refresh() {
        val uiModel = useCaseResult.value
        uiModel?.refresh?.invoke()
    }

    /**
     * Triggers use case to perform a retry operation
     */
    override fun retry() {
        val uiModel = useCaseResult.value
        uiModel?.retry?.invoke()
    }
}