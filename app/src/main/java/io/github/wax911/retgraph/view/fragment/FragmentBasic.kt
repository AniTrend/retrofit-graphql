package io.github.wax911.retgraph.view.fragment

import android.app.Activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.wax911.library.model.request.QueryContainerBuilder
import io.github.wax911.retgraph.R
import io.github.wax911.retgraph.adapter.AdapterExample
import io.github.wax911.retgraph.api.WebFactory
import io.github.wax911.retgraph.api.retro.request.IndexModel
import io.github.wax911.retgraph.model.container.TrendingFeed
import io.github.wax911.retgraph.viewmodel.BasicViewModel
import retrofit2.Call
import retrofit2.Response

class FragmentBasic : Fragment(), Observer<TrendingFeed> {

    private var viewModel: BasicViewModel? = null
    private val adapterExample by lazy {
        AdapterExample()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this)
                .get(BasicViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_basic, container, false)
        val recyclerView = root.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapterExample
        recyclerView.setHasFixedSize(true)
        return root
    }

    /**
     * Called when the Fragment is visible to the user.  This is generally
     * tied to [Activity.onStart] of the containing
     * Activity's lifecycle.
     */
    override fun onStart() {
        super.onStart()
        if (adapterExample.isDataSetEmpty) {
            val queryContainerBuilder = QueryContainerBuilder()
                    .putVariable("type", TrendingFeed.TOP)
                    .putVariable("limit", 20)
                    .putVariable("offset", 1)
            val indexModel = WebFactory.createService(IndexModel::class.java, context)
            viewModel?.makeNewRequest(indexModel.getTrending(queryContainerBuilder))
            Toast.makeText(context, "Loading..", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to [Activity.onResume] of the containing
     * Activity's lifecycle.
     */
    override fun onResume() {
        viewModel?.apply {
            if (!mutableLiveData.hasActiveObservers())
                mutableLiveData.observe(this@FragmentBasic, this@FragmentBasic)
        }
        super.onResume()
    }

    /**
     * Called when the Fragment is no longer resumed.  This is generally
     * tied to [Activity.onPause] of the containing
     * Activity's lifecycle.
     */
    override fun onPause() {
        viewModel?.apply {
            if (mutableLiveData.hasActiveObservers())
                mutableLiveData.removeObserver(this@FragmentBasic)
        }
        super.onPause()
    }

    private fun showError() {
        Toast.makeText(context, "Showing error, about something that failed", Toast.LENGTH_SHORT).show()
    }

    /**
     * Called when the data is changed.
     * @see BasicViewModel.onResponse
     * @param trendingFeed The new data from the view model
     */
    override fun onChanged(trendingFeed: TrendingFeed?) {
        if (trendingFeed != null)
            adapterExample.addItems(trendingFeed.feed)
        else
            showError()
    }

    companion object {
        fun newInstance(): FragmentBasic {
            return FragmentBasic()
        }
    }
}