package co.anitrend.retrofit.graphql.sample.view.content.bucket

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import co.anitrend.arch.domain.entities.LoadState
import co.anitrend.arch.domain.entities.RequestError
import co.anitrend.arch.extension.dispatchers.contract.ISupportDispatcher
import co.anitrend.arch.recycler.adapter.SupportAdapter
import co.anitrend.arch.ui.fragment.list.contract.ISupportFragmentList
import co.anitrend.arch.ui.fragment.list.presenter.SupportListPresenter
import co.anitrend.arch.ui.view.widget.contract.ISupportStateLayout
import co.anitrend.arch.ui.view.widget.model.StateLayoutConfig
import co.anitrend.retrofit.graphql.core.view.SampleListFragment
import co.anitrend.retrofit.graphql.domain.entities.bucket.BucketFile
import co.anitrend.retrofit.graphql.sample.R
import co.anitrend.retrofit.graphql.sample.databinding.BucketContentBinding
import co.anitrend.retrofit.graphql.sample.presenter.BucketPresenter
import co.anitrend.retrofit.graphql.sample.view.content.bucket.viewmodel.BucketViewModel
import co.anitrend.retrofit.graphql.sample.view.content.bucket.viewmodel.UploadViewModel
import io.wax911.emojify.EmojiManager
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class BucketContent(
    override val defaultSpanSize: Int = co.anitrend.arch.theme.R.integer.grid_list_x3,
    override val stateConfig: StateLayoutConfig,
    override val supportViewAdapter: SupportAdapter<BucketFile>,
    override val inflateLayout: Int = R.layout.bucket_content,
) : SampleListFragment<BucketFile>() {

    override val listPresenter = object : SupportListPresenter<BucketFile>() {
        override val recyclerView: RecyclerView
            get() = binding.supportRecyclerView
        override val stateLayout: ISupportStateLayout
            get() = binding.supportStateLayout
        override val swipeRefreshLayout: SwipeRefreshLayout
            get() = binding.supportRefreshLayout
    }

    private lateinit var binding: BucketContentBinding
    private val bucketViewModel by viewModel<BucketViewModel>()
    private val uploadViewModel by viewModel<UploadViewModel>()
    private val presenter by inject<BucketPresenter>()
    private val dispatchers by inject<ISupportDispatcher>()

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) // when the user has not picked anything
                lifecycleScope.launch {
                    val mutation = withContext (dispatchers.io) {
                        presenter.resolve(uri, requireActivity().contentResolver)
                    }
                    mutation?.also(uploadViewModel::invoke) ?: Toast.makeText(
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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                binding.uploadStateLayout.interactionFlow
                    .debounce(16)
                    .filterNotNull()
                    .collect { uploadViewModel.retry() }
            }
        }
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
    ): View {
        binding = BucketContentBinding.inflate(inflater, container, false)
        listPresenter.onCreateView(this, binding.root)
        return binding.root
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
        bucketViewModel()
    }

    /**
     * Invoke view model observer to watch for changes, this will be called
     * called in [onViewCreated]
     */
    override fun setUpViewModelObserver() {
        uploadViewModel.combinedLoadState.observe(
            viewLifecycleOwner
        ) {
            binding.uploadStateLayout.loadStateFlow.value = it
        }
        uploadViewModel.model.observe(
            viewLifecycleOwner
        ) { viewModelState().invoke() }

        viewModelState().model.observe(
            viewLifecycleOwner
        ) { onPostModelChange(it) }

        viewModelState().combinedLoadState.observe(viewLifecycleOwner) {
            if (it is LoadState.Error) {
                binding.supportStateLayout.loadStateFlow.value = presenter.loadStateFailure()
            }
        }
    }

    /**
     * Proxy for a view model state if one exists
     */
    override fun viewModelState() = bucketViewModel

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