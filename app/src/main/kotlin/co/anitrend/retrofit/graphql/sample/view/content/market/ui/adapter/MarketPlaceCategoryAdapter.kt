package co.anitrend.retrofit.graphql.sample.view.content.market.ui.adapter

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import co.anitrend.arch.core.model.IStateLayoutConfig
import co.anitrend.arch.recycler.action.contract.ISupportSelectionMode
import co.anitrend.arch.recycler.adapter.SupportListAdapter
import co.anitrend.arch.recycler.model.contract.IRecyclerItem
import co.anitrend.arch.theme.animator.contract.AbstractAnimator
import co.anitrend.arch.ui.view.widget.model.StateLayoutConfig
import co.anitrend.retrofit.graphql.sample.view.content.market.ui.controller.helpers.CATEGORY_DIFFER
import co.anitrend.retrofit.graphql.sample.view.content.market.ui.controller.model.MarketPlaceCategoryItem
import co.anitrend.retrofit.graphql.sample.view.content.market.ui.controller.model.MarketPlaceCategoryItem.Companion.createViewHolder

class MarketPlaceCategoryAdapter(
    override val resources: Resources,
    override val mapper: (String) -> IRecyclerItem = ::MarketPlaceCategoryItem,
    override val stateConfiguration: IStateLayoutConfig = StateLayoutConfig(),
    override val supportAction: ISupportSelectionMode<Long>? = null,
    override val customSupportAnimator: AbstractAnimator? = null,
) : SupportListAdapter<String>(CATEGORY_DIFFER) {
    /**
     * Should provide the required view holder, this function is a substitute for
     * [androidx.recyclerview.widget.RecyclerView.Adapter.onCreateViewHolder] which now
     * has extended functionality
     */
    override fun createDefaultViewHolder(
        parent: ViewGroup,
        viewType: Int,
        layoutInflater: LayoutInflater
    ) = layoutInflater.createViewHolder(parent)

    /**
     * Used to get stable ids for [androidx.recyclerview.widget.RecyclerView.Adapter] but only if
     * [androidx.recyclerview.widget.RecyclerView.Adapter.setHasStableIds] is set to true.
     *
     * The identifiable id of each item should unique, and if non exists
     * then this function should return [androidx.recyclerview.widget.RecyclerView.NO_ID]
     */
    override fun getStableIdFor(item: String?): Long {
        return item?.hashCode()?.toLong() ?: RecyclerView.NO_ID
    }
}