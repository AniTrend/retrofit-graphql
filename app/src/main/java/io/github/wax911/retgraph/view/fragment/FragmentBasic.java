package io.github.wax911.retgraph.view.fragment;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import io.github.wax911.library.model.request.QueryContainerBuilder;
import io.github.wax911.retgraph.R;
import io.github.wax911.retgraph.adapter.AdapterExample;
import io.github.wax911.retgraph.api.WebFactory;
import io.github.wax911.retgraph.api.retro.request.IndexModel;
import io.github.wax911.retgraph.model.container.TrendingFeed;
import io.github.wax911.retgraph.viewmodel.BasicViewModel;
import retrofit2.Call;
import retrofit2.Response;

public class FragmentBasic extends Fragment implements Observer<TrendingFeed> {

    private BasicViewModel viewModel;
    private AdapterExample adapterExample;

    public static FragmentBasic newInstance() {
        return new FragmentBasic();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(this)
                .get(BasicViewModel.class);
        adapterExample = new AdapterExample();
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_basic, container, false);
        RecyclerView recyclerView = root.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapterExample);
        recyclerView.setHasFixedSize(true);
        return root;
    }

    /**
     * Called when the Fragment is visible to the user.  This is generally
     * tied to {@link Activity#onStart() Activity.onStart} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onStart() {
        super.onStart();
        if(adapterExample.isDataSetEmpty()) {
            QueryContainerBuilder queryContainerBuilder = new QueryContainerBuilder()
                    .putVariable("type", TrendingFeed.TOP)
                    .putVariable("limit", 20)
                    .putVariable("offset", 1);
            IndexModel indexModel = WebFactory.createService(IndexModel.class, getContext());
            viewModel.makeNewRequest(indexModel.getTrending(queryContainerBuilder));
            Toast.makeText(getContext(), "Loading..", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to {@link Activity#onResume() Activity.onResume} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onResume() {
        if(viewModel != null && !viewModel.getMutableLiveData().hasActiveObservers())
            viewModel.getMutableLiveData().observe(this,this);
        super.onResume();
    }

    /**
     * Called when the Fragment is no longer resumed.  This is generally
     * tied to {@link Activity#onPause() Activity.onPause} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onPause() {
        if(viewModel != null && viewModel.getMutableLiveData().hasActiveObservers())
            viewModel.getMutableLiveData().removeObserver(this);
        super.onPause();
    }

    private void showError() {
        Toast.makeText(getContext(), "Showing error, about something that failed", Toast.LENGTH_SHORT).show();
    }

    /**
     * Called when the data is changed.
     * @see BasicViewModel#onResponse(Call, Response)
     *
     * @param trendingFeed The new data from the view model
     */
    @Override
    public void onChanged(@Nullable TrendingFeed trendingFeed) {
        if(trendingFeed != null)
            adapterExample.addItems(trendingFeed.getFeed());
        else
            showError();
    }
}