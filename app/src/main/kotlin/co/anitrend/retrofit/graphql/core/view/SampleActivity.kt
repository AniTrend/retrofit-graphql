package co.anitrend.retrofit.graphql.core.view

import android.os.Build
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import co.anitrend.arch.core.model.ISupportViewModelState
import co.anitrend.arch.extension.ext.getCompatColor
import co.anitrend.arch.ui.activity.SupportActivity
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.fragment.android.setupKoinFragmentFactory
import org.koin.androidx.scope.activityRetainedScope
import org.koin.core.component.KoinScopeComponent
import timber.log.Timber

abstract class SampleActivity : SupportActivity(), AndroidScopeComponent, KoinScopeComponent {

    override val scope by activityRetainedScope()

    /**
     * Can be used to configure custom theme styling as desired
     */
    override fun configureActivity() {
        runCatching {
            Timber.v("Setting up fragment factory using scope: $scope")
            setupKoinFragmentFactory(scope)
        }.onFailure {
            setupKoinFragmentFactory()
            Timber.v(it, "Reverting to scope-less based fragment factory")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val systemUiOptions = window.decorView.systemUiVisibility
            when (AppCompatDelegate.getDefaultNightMode()) {
                AppCompatDelegate.MODE_NIGHT_NO -> {
                    window.navigationBarColor = getCompatColor(co.anitrend.arch.theme.R.color.colorPrimary)
                    window.decorView.systemUiVisibility = systemUiOptions or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                }
                AppCompatDelegate.MODE_NIGHT_YES -> {
                    window.navigationBarColor = getCompatColor(co.anitrend.arch.theme.R.color.colorPrimary)
                    window.decorView.systemUiVisibility = systemUiOptions and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    window.decorView.systemUiVisibility = systemUiOptions and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
                else -> {
                    // According to Google/IO other ui options like auto and follow system might be deprecated
                }
            }
        }
    }

    /**
     * Proxy for a view model state if one exists
     */
    override fun viewModelState(): ISupportViewModelState<*>? = null
}