package co.anitrend.retrofit.graphql.sample.view.content.market.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.PagedList
import co.anitrend.arch.core.model.ISupportViewModelState
import co.anitrend.arch.data.state.DataState
import co.anitrend.retrofit.graphql.data.market.usecase.MarketPlaceUseCaseContract
import co.anitrend.retrofit.graphql.domain.entities.market.MarketPlaceListing
import kotlinx.coroutines.flow.merge

class MarketPlaceViewModel(
    private val useCase: MarketPlaceUseCaseContract
) : ViewModel(), ISupportViewModelState<PagedList<MarketPlaceListing>> {

    private val state = MutableLiveData<DataState<PagedList<MarketPlaceListing>>>()

    override val model = state.switchMap {
        it.model.asLiveData(viewModelScope.coroutineContext)
    }

    override val loadState = state.switchMap {
        it.loadState.asLiveData(viewModelScope.coroutineContext)
    }

    override val refreshState = state.switchMap {
        it.refreshState.asLiveData(viewModelScope.coroutineContext)
    }

    val combinedLoadState = state.switchMap {
        val result = merge(it.loadState, it.refreshState)
        result.asLiveData(viewModelScope.coroutineContext)
    }

    operator fun invoke() {
        val result = useCase()
        state.postValue(result)
    }

    /**
     * Triggers use case to perform refresh operation
     */
    override suspend fun refresh() {
        val uiModel = state.value
        uiModel?.refresh?.invoke()
    }

    /**
     * Triggers use case to perform a retry operation
     */
    override suspend fun retry() {
        val uiModel = state.value
        uiModel?.retry?.invoke()
    }

    override fun onCleared() {
        useCase.onCleared()
        super.onCleared()
    }
}
