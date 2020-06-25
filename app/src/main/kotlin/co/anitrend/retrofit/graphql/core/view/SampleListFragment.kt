package co.anitrend.retrofit.graphql.core.view

import androidx.lifecycle.Observer
import co.anitrend.arch.domain.entities.NetworkState
import co.anitrend.arch.ui.fragment.list.SupportFragmentList

abstract class SampleListFragment<M : Any> : SupportFragmentList<M>() {
    override val onRefreshObserver = Observer<NetworkState> {
        // workaround for support-arch:ui on refresh overrides network state
    }
}