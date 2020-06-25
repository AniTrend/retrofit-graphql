package co.anitrend.retrofit.graphql.core.view

import android.os.Build
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import co.anitrend.arch.extension.ext.getCompatColor
import co.anitrend.arch.ui.R
import co.anitrend.arch.ui.activity.SupportActivity
import org.koin.androidx.fragment.android.setupKoinFragmentFactory
import org.koin.androidx.scope.lifecycleScope as koinLifecycleScope

abstract class SampleActivity : SupportActivity() {

    /**
     * Can be used to configure custom theme styling as desired
     */
    override fun configureActivity() {
        setupKoinFragmentFactory(koinLifecycleScope)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val systemUiOptions = window.decorView.systemUiVisibility
            when (AppCompatDelegate.getDefaultNightMode()) {
                AppCompatDelegate.MODE_NIGHT_NO -> {
                    window.navigationBarColor = getCompatColor(R.color.colorPrimary)
                    window.decorView.systemUiVisibility = systemUiOptions or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                }
                AppCompatDelegate.MODE_NIGHT_YES -> {
                    window.navigationBarColor = getCompatColor(R.color.colorPrimary)
                    window.decorView.systemUiVisibility = systemUiOptions and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    window.decorView.systemUiVisibility = systemUiOptions and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
                else -> {
                    // According to Google/IO other ui options like auto and follow system might be deprecated
                }
            }
        }
    }
}