package co.anitrend.retrofit.graphql.sample.view.content.bucket.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import co.anitrend.arch.core.model.ISupportViewModelState
import co.anitrend.arch.data.state.DataState
import co.anitrend.retrofit.graphql.data.bucket.model.upload.mutation.UploadMutation
import co.anitrend.retrofit.graphql.data.bucket.usecase.upload.UploadUseCaseContract
import co.anitrend.retrofit.graphql.domain.entities.bucket.BucketFile
import kotlinx.coroutines.flow.merge

class UploadViewModel(
    private val useCase: UploadUseCaseContract
) : ViewModel(), ISupportViewModelState<BucketFile> {

    private val state = MutableLiveData<DataState<BucketFile>>()

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

    operator fun invoke(mutation: UploadMutation) {
        val result = useCase(mutation)
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