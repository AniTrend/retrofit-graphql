package io.github.wax911.retgraph.viewmodel

import android.util.Log

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.wax911.library.model.body.GraphContainer
import io.github.wax911.library.util.getError
import io.github.wax911.retgraph.model.container.TrendingFeed
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * View Model basic example
 */
class TrendingViewModel : ViewModel(), Callback<GraphContainer<TrendingFeed>> {

    val mutableLiveData: MutableLiveData<TrendingFeed> by lazy {
        MutableLiveData<TrendingFeed>()
    }

    private var graphContainerCall: Call<GraphContainer<TrendingFeed>>? = null

    fun makeNewRequest(graphContainerCall: Call<GraphContainer<TrendingFeed>>) {
        this.graphContainerCall = graphContainerCall.apply {
            enqueue(this@TrendingViewModel)
        }
    }

    /**
     * Invoked for a received HTTP response.
     *
     *
     * Note: An HTTP response may still indicate an application-level failure such as a 404 or 500.
     * Call [Response.isSuccessful] to determine if the response indicates success.
     *
     * @param call
     * @param response
     */
    override fun onResponse(call: Call<GraphContainer<TrendingFeed>>, response: Response<GraphContainer<TrendingFeed>>) {
        val container: GraphContainer<TrendingFeed>? = response.body()
        if (response.isSuccessful && container != null) {
            if (!container.isEmpty())
                mutableLiveData.value = container.data
        } else {
                response.getError()?.apply {
                forEach { Log.e(this@TrendingViewModel.toString(), it.toString()) }
            }
        }
    }

    /**
     * Invoked when a network exception occurred talking to the server or when an unexpected
     * exception occurred creating the request or processing the response.
     *
     * @param call
     * @param throwable
     */
    override fun onFailure(call: Call<GraphContainer<TrendingFeed>>, throwable: Throwable) {
        throwable.printStackTrace()
        mutableLiveData.value = null
    }

    /**
     * This method will be called when this ViewModel is no longer used and will be destroyed.
     *
     *
     * It is useful when ViewModel observes some data and you need to clear this subscription to
     * prevent a leak of this ViewModel.
     */
    override fun onCleared() {
        graphContainerCall?.apply {
            if (isExecuted)
                cancel()
        }
        super.onCleared()
    }
}
