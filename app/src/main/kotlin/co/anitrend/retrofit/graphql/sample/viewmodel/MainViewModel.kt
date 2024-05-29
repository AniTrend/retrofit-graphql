package co.anitrend.retrofit.graphql.sample.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import co.anitrend.arch.core.model.ISupportViewModelState
import co.anitrend.arch.data.state.DataState
import co.anitrend.retrofit.graphql.data.user.usecase.UserUseCaseContract
import co.anitrend.retrofit.graphql.domain.entities.user.User
import kotlinx.coroutines.flow.merge

class MainViewModel(
    private val useCase: UserUseCaseContract
) : ViewModel(), ISupportViewModelState<User> {

    private val state = MutableLiveData<DataState<User>>()

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