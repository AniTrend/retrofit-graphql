package io.github.wax911.retgraph.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.map
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wax911.library.model.body.GraphContainer
import io.github.wax911.library.model.request.QueryContainerBuilder
import io.github.wax911.library.util.getError
import io.github.wax911.retgraph.api.retro.request.IndexModel
import io.github.wax911.retgraph.model.container.TrendingFeed
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import retrofit2.Response

/**
 * View Model basic example
 */
class TrendingViewModel(
        private val service: IndexModel,
        private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _liveData = MutableLiveData<TrendingFeed?>()

    val model: LiveData<TrendingFeed?> =
            map(_liveData) {
                it
            }

    operator fun invoke(queryContainerBuilder: QueryContainerBuilder) {
        viewModelScope.launch(dispatcher) {
            try {
                val response =
                        service.getTrending(queryContainerBuilder)
                onResponse(response)
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    private fun onResponse(response: Response<GraphContainer<TrendingFeed>>) {
        val container: GraphContainer<TrendingFeed>? = response.body()
        if (response.isSuccessful && container != null) {
            if (container.data?.feed?.isEmpty() != true)
                _liveData.postValue(container.data)
        } else {
                response.getError()?.apply {
                forEach { Log.e(this@TrendingViewModel.toString(), it.toString()) }
            }
        }
    }

    private fun onFailure(throwable: Throwable) {
        throwable.printStackTrace()
        _liveData.postValue(null)
    }
}
