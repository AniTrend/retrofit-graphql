package io.github.wax911.retgraph.viewmodel;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;

import io.github.wax911.library.model.attribute.GraphError;
import io.github.wax911.library.model.body.GraphContainer;
import io.github.wax911.library.util.GraphErrorUtil;
import io.github.wax911.retgraph.model.container.TrendingFeed;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * View Model basic example
 */
public class BasicViewModel extends ViewModel implements Callback<GraphContainer<TrendingFeed>> {

    private @Nullable MutableLiveData<TrendingFeed> mutableLiveData;
    private Call<GraphContainer<TrendingFeed>> graphContainerCall;

    public @NonNull MutableLiveData<TrendingFeed> getMutableLiveData() {
        if(mutableLiveData == null)
            mutableLiveData = new MutableLiveData<>();
        return mutableLiveData;
    }

    public void makeNewRequest(Call<GraphContainer<TrendingFeed>> graphContainerCall) {
        this.graphContainerCall = graphContainerCall;
        this.graphContainerCall.enqueue(this);
    }

    /**
     * Invoked for a received HTTP response.
     * <p>
     * Note: An HTTP response may still indicate an application-level failure such as a 404 or 500.
     * Call {@link Response#isSuccessful()} to determine if the response indicates success.
     *
     * @param call
     * @param response
     */
    @Override
    public void onResponse(@NonNull Call<GraphContainer<TrendingFeed>> call, @NonNull Response<GraphContainer<TrendingFeed>> response) {
        GraphContainer<TrendingFeed> container;
        if(response.isSuccessful() && (container = response.body()) != null) {
            if(!container.isEmpty())
                getMutableLiveData().setValue(container.getData());
        } else {
            List<GraphError> graphErrors = GraphErrorUtil.getError(response);
            if (graphErrors != null)
                for (GraphError graphError : graphErrors)
                    Log.e(toString(), graphError.toString());
        }
    }

    /**
     * Invoked when a network exception occurred talking to the server or when an unexpected
     * exception occurred creating the request or processing the response.
     *
     * @param call
     * @param throwable
     */
    @Override
    public void onFailure(@NonNull Call<GraphContainer<TrendingFeed>> call, @NonNull Throwable throwable) {
        throwable.printStackTrace();
        getMutableLiveData().setValue(null);
    }

    /**
     * This method will be called when this ViewModel is no longer used and will be destroyed.
     * <p>
     * It is useful when ViewModel observes some data and you need to clear this subscription to
     * prevent a leak of this ViewModel.
     */
    @Override
    protected void onCleared() {
        if(graphContainerCall != null && !graphContainerCall.isExecuted())
            graphContainerCall.cancel();
        super.onCleared();
    }
}
