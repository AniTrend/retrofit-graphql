package co.anitrend.retrofit.graphql.sample.view.content.bucket

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import co.anitrend.arch.domain.entities.NetworkState
import co.anitrend.arch.domain.extensions.isSuccess
import co.anitrend.arch.extension.dispatchers.SupportDispatchers
import co.anitrend.arch.recycler.adapter.contract.ISupportAdapter
import co.anitrend.arch.ui.view.widget.model.StateLayoutConfig
import co.anitrend.retrofit.graphql.core.view.SampleListFragment
import co.anitrend.retrofit.graphql.domain.entities.bucket.BucketFile
import co.anitrend.retrofit.graphql.sample.R
import co.anitrend.retrofit.graphql.sample.databinding.BucketContentBinding
import co.anitrend.retrofit.graphql.sample.extensions.emojify
import co.anitrend.retrofit.graphql.sample.presenter.BucketPresenter
import co.anitrend.retrofit.graphql.sample.view.content.bucket.viewmodel.BucketViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.androidx.scope.lifecycleScope as koinScope

class BucketContent(
    override val defaultSpanSize: Int = co.anitrend.arch.ui.R.integer.grid_list_x3,
    override val stateConfig: StateLayoutConfig,
    override val supportViewAdapter: ISupportAdapter<BucketFile>,
    override val inflateLayout: Int = R.layout.bucket_content
) : SampleListFragment<BucketFile>() {

    private lateinit var binding: BucketContentBinding
    private val viewModel by viewModel<BucketViewModel>()
    private val presenter by inject<BucketPresenter>()
    private val dispatchers by inject<SupportDispatchers>()

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) // when the user has not picked anything
                lifecycleScope.launch {
                    val mutation = withContext (dispatchers.io) {
                        presenter.resolve(uri, requireActivity().contentResolver)
                    }
                    if (mutation != null)
                        viewModel.uploadState(mutation)
                    else
                        Toast.makeText(
                            context,
                            "Unable to resolve content",
                            Toast.LENGTH_SHORT
                        ).show()
                }
        }

    /**
     * Additional initialization to be done in this method, this method will be called in
     * [androidx.fragment.app.FragmentActivity.onCreate].
     *
     * @param savedInstanceState
     */
    override fun initializeComponents(savedInstanceState: Bundle?) {
        super.initializeComponents(savedInstanceState)
        lifecycleScope.launchWhenResumed {
            binding.uploadStateLayout.interactionStateFlow
                .debounce(16)
                .filterNotNull()
                .collect { viewModel.uploadState.retry() }
        }
    }

    /**
     * Called immediately after [onCreateView] has returned, but before any saved state has been
     * restored in to the view. This gives subclasses a chance to initialize themselves once
     * they know their view hierarchy has been completely created. The fragment's view hierarchy
     * is not however attached to its parent at this point.
     *
     * @param view The View returned by [.onCreateView].
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = BucketContentBinding.bind(view)
        binding.uploadStateLayout.stateConfigFlow.value = stateConfig
        binding.selectUploadFile.setOnClickListener {
            // filter to only allow images to be selected
            activityResultLauncher.launch("image/*")
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
        viewModel.bucketState()
    }

    /**
     * Informs the underlying [SupportStateLayout] of changes to the [NetworkState]
     *
     * @param networkState New state from the application
     */
    @ExperimentalCoroutinesApi
    override fun changeLayoutState(networkState: NetworkState?) {
        super.changeLayoutState(networkState)
        if (networkState?.isSuccess() != true) return
        val emoji = context?.emojify()?.getForAlias("gallery")
        binding.supportStateLayout.networkMutableStateFlow.value = NetworkState.Error(
            heading = "${emoji?.unicode} No images found",
            message = "Try to upload some pictures and they will show up here"
        )
    }

    /**
     * Invoke view model observer to watch for changes, this will be called
     * called in [onViewCreated]
     */
    override fun setUpViewModelObserver() {
        val uploadStateObserver = Observer<NetworkState> {
            binding.uploadStateLayout.networkMutableStateFlow.value = it
        }
        viewModel.uploadState.networkState.observe(
            viewLifecycleOwner,
            uploadStateObserver
        )
        viewModel.uploadState.refreshState.observe(
            viewLifecycleOwner,
            uploadStateObserver
        )
        viewModel.uploadState.model.observe(
            viewLifecycleOwner,
            Observer { viewModelState().refresh() }
        )
        viewModelState().model.observe(
            viewLifecycleOwner,
            Observer { onPostModelChange(it) }
        )
    }

    /**
     * Proxy for a view model state if one exists
     */
    override fun viewModelState() = viewModel.bucketState

    /**
     * Called when the view previously created by [onCreateView] has
     * been detached from the fragment. The next time the fragment needs
     * to be displayed, a new view will be created.
     *
     * This is called after [onStop] and before [onDestroy]. It is called *regardless* of
     * whether [onCreateView] returned a non-null view. Internally it is called after the
     * view's state has been saved but before it has been removed from its parent.
     */
    override fun onDestroyView() {
        binding.selectUploadFile.setOnClickListener(null)
        super.onDestroyView()
    }
}