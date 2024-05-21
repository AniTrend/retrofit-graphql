package co.anitrend.retrofit.graphql.sample.view

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import co.anitrend.arch.domain.entities.NetworkState
import co.anitrend.arch.extension.ext.LAZY_MODE_UNSAFE
import co.anitrend.retrofit.graphql.core.extension.commit
import co.anitrend.retrofit.graphql.core.model.FragmentItem
import co.anitrend.retrofit.graphql.core.view.SampleActivity
import co.anitrend.retrofit.graphql.sample.R
import co.anitrend.retrofit.graphql.sample.databinding.ActivityMainBinding
import co.anitrend.retrofit.graphql.sample.databinding.NavHeaderMainBinding
import co.anitrend.retrofit.graphql.sample.presenter.MainPresenter
import co.anitrend.retrofit.graphql.sample.view.content.bucket.BucketContent
import co.anitrend.retrofit.graphql.sample.view.content.market.MarketPlaceContent
import co.anitrend.retrofit.graphql.sample.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MainScreen : SampleActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val binding by lazy(LAZY_MODE_UNSAFE) {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val bottomDrawerBehavior by lazy(LAZY_MODE_UNSAFE) {
        BottomSheetBehavior.from(binding.bottomNavigationDrawer)
    }

    @IdRes private var selectedItem: Int = R.id.nav_app_store
    @StringRes private var selectedTitle: Int = R.string.nav_app_store

    private val viewModel by viewModel<MainViewModel>()

    private val presenter by inject<MainPresenter>()

    private val headerBinding by lazy(LAZY_MODE_UNSAFE) {
        val headerView = binding.bottomNavigationView.getHeaderView(0)
        NavHeaderMainBinding.bind(headerView)
    }

    /**
     * Additional initialization to be done in this method, this is called in during
     * [androidx.fragment.app.FragmentActivity.onPostCreate]
     *
     * @param savedInstanceState
     */
    override fun initializeComponents(savedInstanceState: Bundle?) {
        binding.bottomAppBar.apply {
            setNavigationOnClickListener {
                bottomDrawerBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
            }
        }
        binding.bottomNavigationView.apply {
            setCheckedItem(selectedItem)
            setNavigationItemSelectedListener(this@MainScreen)
            val observer = Observer<NetworkState> {
                headerBinding.navStateLayout.networkMutableStateFlow.value = it
            }
            presenter.configureNavigationHeader(headerBinding)
            viewModel.state.networkState.observe(this@MainScreen, observer)
            viewModel.state.refreshState.observe(this@MainScreen, observer)
            viewModel.state.model.observe(this@MainScreen, Observer {
                presenter.updateNavigationHeaderView(it, headerBinding)
            })
        }
        updateUserInterface()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.bottomAppBar)
        bottomDrawerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(key_navigation_selected, selectedItem)
        outState.putInt(key_navigation_title, selectedTitle)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        selectedItem = savedInstanceState.getInt(key_navigation_selected)
        selectedTitle = savedInstanceState.getInt(key_navigation_title)
    }

    override fun onBackPressed() {
        when (bottomDrawerBehavior.state) {
            BottomSheetBehavior.STATE_EXPANDED ->
                bottomDrawerBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
            BottomSheetBehavior.STATE_HALF_EXPANDED,
            BottomSheetBehavior.STATE_COLLAPSED ->
                bottomDrawerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            else -> super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> {
                Toast.makeText(this, "search clicked", Toast.LENGTH_SHORT).show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Called when an item in the navigation menu is selected.
     *
     * @param item The selected item
     * @return true to display the item as the selected item
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (selectedItem != item.itemId) {
            selectedItem = item.itemId
            onNavigateToTarget(selectedItem)
        }
        return true
    }


    private fun onNavigate(@IdRes menu: Int) {
        var fragmentItem: FragmentItem<*>? = null
        when (menu) {
            R.id.nav_app_store -> {
                selectedTitle = R.string.nav_app_store
                fragmentItem = FragmentItem(
                    fragment = MarketPlaceContent::class.java
                )
            }
            R.id.nav_uploads -> {
                selectedTitle = R.string.nav_uploads
                fragmentItem = FragmentItem(
                    fragment = BucketContent::class.java
                )
            }
        }

        binding.bottomAppBar.setTitle(selectedTitle)
        bottomDrawerBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        currentFragmentTag = supportFragmentManager.commit(
            binding.activityContent.contentFrame.id, fragmentItem
        ) {}
    }

    private fun onNavigateToTarget(@IdRes menu: Int) {
        lifecycleScope.launch {
            runCatching {
                onNavigate(menu)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun updateUserInterface() {
        lifecycleScope.launchWhenResumed {
            val stateLayout = headerBinding.navStateLayout
            stateLayout.interactionStateFlow.collect { viewModel.state.retry() }
        }
        lifecycleScope.launchWhenResumed {
            viewModel.state()
        }
        if (selectedItem != 0) onNavigateToTarget(selectedItem)
        else onNavigateToTarget(R.id.nav_app_store)
    }

    companion object {
        private const val key_navigation_selected = "key_navigation_selected"
        private const val key_navigation_title = "key_navigation_title"
    }
}