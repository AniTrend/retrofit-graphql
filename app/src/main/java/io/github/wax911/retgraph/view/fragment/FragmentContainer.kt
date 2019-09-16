package io.github.wax911.retgraph.view.fragment

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.github.wax911.library.model.request.QueryContainerBuilder
import io.github.wax911.retgraph.R
import io.github.wax911.retgraph.adapter.AdapterExample
import io.github.wax911.retgraph.model.container.TrendingFeed
import io.github.wax911.retgraph.viewmodel.TrendingViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class FragmentContainer : Fragment() {

    private val viewModel by viewModel<TrendingViewModel>()

    private val adapterExample by lazy {
        AdapterExample()
    }

    private var progressBar: ProgressBar? = null
    private var refreshLayout: SwipeRefreshLayout? = null
    private var viewFlipper: ViewFlipper? = null
    private var errorTextView: TextView? = null

    @TrendingFeed.FeedType
    private var feedType: String? = null

    private val observer = Observer<TrendingFeed?> {
        onViewModelResult(it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.also {
            feedType = it.getString(argFeedType)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_trending, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<RecyclerView>(R.id.recycler_view).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = adapterExample
            setHasFixedSize(true)
        }
        viewFlipper = view.findViewById<ViewFlipper>(R.id.view_flipper).apply {
            errorTextView = findViewById<TextView>(R.id.errorMessage).also {
                it.visibility = View.GONE
            }
            progressBar = findViewById<ProgressBar>(R.id.progressBar).also {
                it.visibility = View.VISIBLE
            }
        }
        refreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.refreshLayout).apply {
            setOnRefreshListener {
                when {
                    viewFlipper != null && viewFlipper!!.displayedChild > 0 -> {
                        errorTextView?.visibility = View.GONE
                        if (viewFlipper?.displayedChild == 1)
                            viewFlipper?.showPrevious()
                        startNetworkRequest()
                    }
                    else -> {
                        progressBar?.visibility = View.VISIBLE
                        errorTextView?.visibility = View.GONE
                        startNetworkRequest()
                    }
                }
            }
        }
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to [Activity.onResume] of the containing
     * Activity's lifecycle.
     */
    override fun onResume() {
        viewModel.model.observe(this, observer)
        startNetworkRequest()
        super.onResume()
    }

    /**
     * Called when the Fragment is no longer resumed.  This is generally
     * tied to [Activity.onPause] of the containing
     * Activity's lifecycle.
     */
    override fun onPause() {
        viewModel.model.removeObserver(observer)
        super.onPause()
    }

    private fun startNetworkRequest() {
        if (viewModel.model.value == null) {
            val queryContainerBuilder = QueryContainerBuilder()
                    .putVariable("type", feedType)
                    .putVariable("limit", 20)
                    .putVariable("offset", 1)
            viewModel(queryContainerBuilder)
        } else
            onViewModelResult(viewModel.model.value)
    }

    private fun showError() {
        viewFlipper?.apply {
            errorTextView?.also {
                it.visibility = View.VISIBLE
                it.setText(R.string.connection_error)
            }
            progressBar?.visibility = View.GONE
            if (displayedChild > 0)
                showPrevious()
        }
    }

    /**
     * Called when the data is changed.
     * @see TrendingViewModel.model
     */
    private fun onViewModelResult(result: TrendingFeed?) {
        if (result?.feed != null) {
            viewFlipper?.apply {
                if (displayedChild < 1)
                    showNext()
            }
            adapterExample.apply {
                if (refreshLayout?.isRefreshing == true)
                    model.clear()
                if (model.size < 1)
                    addItems(result.feed)
            }
        }
        else
            showError()
        refreshLayout?.apply {
            if (isRefreshing)
                isRefreshing = false
        }
    }

    companion object {
        const val argFeedType: String = "arg_feed_type"

        fun newInstance(bundle: Bundle?): FragmentContainer {
            return FragmentContainer().apply {
                arguments = bundle
            }
        }
    }
}