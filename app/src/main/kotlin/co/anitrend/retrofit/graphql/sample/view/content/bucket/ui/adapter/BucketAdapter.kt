package co.anitrend.retrofit.graphql.sample.view.content.bucket.ui.adapter

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import co.anitrend.arch.core.model.IStateLayoutConfig
import co.anitrend.arch.recycler.action.contract.ISupportSelectionMode
import co.anitrend.arch.recycler.adapter.SupportListAdapter
import co.anitrend.arch.recycler.model.contract.IRecyclerItem
import co.anitrend.arch.theme.animator.contract.ISupportAnimator
import co.anitrend.retrofit.graphql.domain.entities.bucket.BucketFile
import co.anitrend.retrofit.graphql.sample.view.content.bucket.ui.controller.helper.DIFFER
import co.anitrend.retrofit.graphql.sample.view.content.bucket.ui.controller.model.BucketFileItem
import co.anitrend.retrofit.graphql.sample.view.content.bucket.ui.controller.model.BucketFileItem.Companion.createViewHolder

class BucketAdapter(
    override val customSupportAnimator: ISupportAnimator? = null,
    override val mapper: (BucketFile?) -> IRecyclerItem = { BucketFileItem(it) },
    override val resources: Resources,
    override val stateConfiguration: IStateLayoutConfig,
    override val supportAction: ISupportSelectionMode<Long>? = null
) : SupportListAdapter<BucketFile>(DIFFER) {

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
    override fun getStableIdFor(item: BucketFile?): Long {
        return item?.id?.toLong() ?: RecyclerView.NO_ID
    }
}