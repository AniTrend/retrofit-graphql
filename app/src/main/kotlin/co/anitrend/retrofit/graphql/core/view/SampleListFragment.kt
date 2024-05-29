package co.anitrend.retrofit.graphql.core.view

import co.anitrend.arch.core.model.ISupportViewModelState
import co.anitrend.arch.ui.fragment.list.SupportFragmentList
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.fragmentScope
import org.koin.core.component.KoinScopeComponent

abstract class SampleListFragment<M : Any> : SupportFragmentList<M>(),
    AndroidScopeComponent, KoinScopeComponent {

    override val scope by fragmentScope()

    /**
     * Proxy for a view model state if one exists
     */
    override fun viewModelState(): ISupportViewModelState<*>? = null
}