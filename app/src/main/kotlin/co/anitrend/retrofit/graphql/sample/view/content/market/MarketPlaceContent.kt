package co.anitrend.retrofit.graphql.sample.view.content.market

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import co.anitrend.arch.recycler.adapter.contract.ISupportAdapter
import co.anitrend.arch.recycler.common.DefaultClickableItem
import co.anitrend.arch.ui.view.widget.model.StateLayoutConfig
import co.anitrend.retrofit.graphql.core.view.SampleListFragment
import co.anitrend.retrofit.graphql.domain.entities.market.MarketPlaceListing
import co.anitrend.retrofit.graphql.sample.R
import co.anitrend.retrofit.graphql.sample.view.content.market.viewmodel.MarketPlaceViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterIsInstance
import org.koin.androidx.viewmodel.ext.android.viewModel

class MarketPlaceContent(
    override val defaultSpanSize: Int = R.integer.single_list_size,
    override val stateConfig: StateLayoutConfig,
    override val supportViewAdapter: ISupportAdapter<MarketPlaceListing>
) : SampleListFragment<MarketPlaceListing>() {

    private val viewModel by viewModel<MarketPlaceViewModel>()

    /**
     * Additional initialization to be done in this method, this method will be called in
     * [androidx.fragment.app.FragmentActivity.onCreate].
     *
     * @param savedInstanceState
     */
    override fun initializeComponents(savedInstanceState: Bundle?) {
        super.initializeComponents(savedInstanceState)
        lifecycleScope.launchWhenResumed {
            supportViewAdapter.clickableStateFlow.debounce(16)
                .filterIsInstance<DefaultClickableItem<MarketPlaceListing>>()
                .collect { item ->
                    item.data?.also {
                        Toast.makeText(
                            item.view.context,
                            "Item clicked: ${it.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
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
     * Invoke view model observer to watch for changes, this will be called
     * called in [onViewCreated]
     */
    override fun setUpViewModelObserver() {
        viewModelState().model.observe(
            viewLifecycleOwner,
            Observer {
                onPostModelChange(it)
            }
        )
    }

    /**
     * Proxy for a view model state if one exists
     */
    override fun viewModelState() = viewModel.state
}