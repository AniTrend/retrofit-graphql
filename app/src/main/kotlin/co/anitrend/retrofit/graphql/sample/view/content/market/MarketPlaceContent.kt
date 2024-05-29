package co.anitrend.retrofit.graphql.sample.view.content.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import co.anitrend.arch.extension.ext.getColorFromAttr
import co.anitrend.arch.recycler.SupportRecyclerView
import co.anitrend.arch.recycler.paging.legacy.adapter.SupportPagedListAdapter
import co.anitrend.arch.recycler.shared.adapter.SupportLoadStateAdapter
import co.anitrend.arch.ui.fragment.list.contract.ISupportFragmentList
import co.anitrend.arch.ui.fragment.list.presenter.SupportListPresenter
import co.anitrend.arch.ui.view.widget.contract.ISupportStateLayout
import co.anitrend.arch.ui.view.widget.model.StateLayoutConfig
import co.anitrend.retrofit.graphql.core.view.SampleListFragment
import co.anitrend.retrofit.graphql.domain.entities.market.MarketPlaceListing
import co.anitrend.retrofit.graphql.sample.databinding.SharedListContentBinding
import co.anitrend.retrofit.graphql.sample.view.content.market.viewmodel.MarketPlaceViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MarketPlaceContent(
    override val inflateLayout: Int = co.anitrend.retrofit.graphql.sample.R.layout.shared_list_content,
    override val defaultSpanSize: Int = co.anitrend.arch.theme.R.integer.single_list_size,
    override val stateConfig: StateLayoutConfig,
    override val supportViewAdapter: SupportPagedListAdapter<MarketPlaceListing>
) : SampleListFragment<MarketPlaceListing>() {

    override val listPresenter = object : SupportListPresenter<MarketPlaceListing>() {
        override val recyclerView: RecyclerView
            get() = requireNotNull(binding).recyclerView
        override val stateLayout: ISupportStateLayout
            get() = requireNotNull(binding).stateLayout
        override val swipeRefreshLayout: SwipeRefreshLayout
            get() = requireNotNull(binding).swipeRefreshLayout

        private var binding: SharedListContentBinding? = null

        override fun onCreateView(fragmentList: ISupportFragmentList<MarketPlaceListing>, view: View?) {
            binding = SharedListContentBinding.bind(requireNotNull(view))
            super.onCreateView(fragmentList, view)
        }
    }

    private val viewModel by viewModel<MarketPlaceViewModel>()

    /**
     * Sets the adapter for the recycler view
     */
    override fun setRecyclerAdapter(recyclerView: SupportRecyclerView) {
        if (recyclerView.adapter == null) {
            val header = SupportLoadStateAdapter(resources, stateConfig).apply {
                registerFlowListener()
            }
            val footer = SupportLoadStateAdapter(resources, stateConfig).apply {
                registerFlowListener()
            }

            (supportViewAdapter as RecyclerView.Adapter<*>)
                .stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            recyclerView.adapter = supportViewAdapter.withLoadStateHeaderAndFooter(
                header = header, footer = footer
            )
        }
    }

    /**
     * Stub to trigger the loading of data, by default this is only called
     * when [supportViewAdapter] has no data in its underlying source.
     *
     * This is called when the fragment reaches it's [onStart] state
     *
     * @see initializeComponents
     */
    override fun onFetchDataInitialize() {
        viewModelState().invoke()
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is optional, and non-graphical fragments can return null (which
     * is the default implementation). This will be called between
     * [onCreate] and [onActivityCreated].
     *
     * If you return a View from here, you will later be called in
     * [onDestroyView] when the view is being released.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return Return the [View] for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        listPresenter.swipeRefreshLayout?.setColorSchemeColors(
            requireContext().getColorFromAttr(androidx.appcompat.R.attr.colorPrimary),
            requireContext().getColorFromAttr(androidx.appcompat.R.attr.colorAccent)
        )
        return view
    }

    /**
     * Invoke view model observer to watch for changes, this will be called
     * called in [onViewCreated]
     */
    override fun setUpViewModelObserver() {
        viewModelState().model.observe(
            viewLifecycleOwner,
            ::onPostModelChange
        )
    }

    /**
     * Proxy for a view model state if one exists
     */
    override fun viewModelState() = viewModel
}