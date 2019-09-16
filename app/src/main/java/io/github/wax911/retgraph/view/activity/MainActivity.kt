package io.github.wax911.retgraph.view.activity

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import io.github.wax911.retgraph.R
import io.github.wax911.retgraph.common.ThemedActivity
import io.github.wax911.retgraph.model.container.TrendingFeed
import io.github.wax911.retgraph.view.fragment.FragmentContainer
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : ThemedActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val bottomDrawerBehavior by lazy(LazyThreadSafetyMode.NONE) {
        BottomSheetBehavior.from(bottomDrawer)
    }

    private val navigationView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<NavigationView>(R.id.navigationView)
    }

    private val fab by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<FloatingActionButton>(R.id.fab)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bottomDrawerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        bottomAppBar.apply {
            setNavigationOnClickListener {
                bottomDrawerBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
            }
            replaceMenu(R.menu.main_menu)
        }
        fab.setOnClickListener {
            Toast.makeText(this, "Fab Clicked", Toast.LENGTH_SHORT).show()
        }
        navigationView.apply {
            setNavigationItemSelectedListener(this@MainActivity)
            setCheckedItem(R.id.nav_hot)
            checkedItem?.also { onNavigationItemSelected(it) }
        }
    }

    /**
     * Take care of popping the fragment back stack or finishing the activity
     * as appropriate.
     */
    override fun onBackPressed() {
        if (bottomDrawerBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
            bottomDrawerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            return
        }
        super.onBackPressed()
    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        val fragment: Fragment?
        val result = when (menuItem.itemId) {
            R.id.nav_trending -> {
                fragment = FragmentContainer.newInstance(Bundle().apply {
                    putString(FragmentContainer.argFeedType, TrendingFeed.TOP)
                })
                true
            }
            R.id.nav_hot -> {
                fragment = FragmentContainer.newInstance(Bundle().apply {
                    putString(FragmentContainer.argFeedType, TrendingFeed.HOT)
                })
                true
            }
            R.id.nav_new -> {
                fragment = FragmentContainer.newInstance(Bundle().apply {
                    putString(FragmentContainer.argFeedType, TrendingFeed.NEW)
                })
                true
            }
            else -> {
                fragment = null
                false
            }
        }
        fragment?.also {
            val fragmentManager = supportFragmentManager
            val fragmentTransaction = fragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.frame_content, it, it.toString())
            fragmentTransaction.commit()
        }
        bottomDrawerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        return result
    }
}
