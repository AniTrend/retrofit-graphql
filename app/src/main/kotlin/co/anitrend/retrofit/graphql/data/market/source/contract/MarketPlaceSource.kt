package co.anitrend.retrofit.graphql.data.market.source.contract

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import co.anitrend.arch.extension.dispatchers.SupportDispatchers
import co.anitrend.retrofit.graphql.data.arch.common.SamplePagedSource
import co.anitrend.retrofit.graphql.domain.entities.market.MarketPlaceListing

internal abstract class MarketPlaceSource(
    dispatchers: SupportDispatchers
) : SamplePagedSource<MarketPlaceListing>(dispatchers) {

    protected abstract val observable: LiveData<PagedList<MarketPlaceListing>>

    operator fun invoke() = observable
}