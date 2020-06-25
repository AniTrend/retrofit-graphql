package co.anitrend.retrofit.graphql.sample.view.content.market.ui.controller.model

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import co.anitrend.arch.recycler.action.contract.ISupportSelectionMode
import co.anitrend.arch.recycler.common.ClickableItem
import co.anitrend.arch.recycler.holder.SupportViewHolder
import co.anitrend.arch.recycler.model.RecyclerItem
import co.anitrend.retrofit.graphql.sample.R
import co.anitrend.retrofit.graphql.sample.databinding.MarketPlaceCategoryItemBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

class MarketPlaceCategoryItem(
    private val entity: String?
) : RecyclerItem(entity?.hashCode()?.toLong()) {

    private var binding: MarketPlaceCategoryItemBinding? = null

    /**
     * Called when the [view] needs to be setup, this could be to set click listeners,
     * assign text, load images, e.t.c
     *
     * @param view view that was inflated
     * @param position current position
     * @param payloads optional payloads which maybe empty
     * @param stateFlow observable to broadcast click events
     * @param selectionMode action mode helper or null if none was provided
     */
    @ExperimentalCoroutinesApi
    override fun bind(
        view: View,
        position: Int,
        payloads: List<Any>,
        stateFlow: MutableStateFlow<ClickableItem?>,
        selectionMode: ISupportSelectionMode<Long>?
    ) {
        binding = MarketPlaceCategoryItemBinding.bind(view)
        binding?.listingCategory?.text = entity
    }

    /**
     * Provides a preferred span size for the item
     *
     * @param spanCount current span count which may also be [INVALID_SPAN_COUNT]
     * @param position position of the current item
     * @param resources optionally useful for dynamic size check with different configurations
     */
    override fun getSpanSize(spanCount: Int, position: Int, resources: Resources): Int {
        return resources.getInteger(R.integer.single_list_size)
    }

    /**
     * Called when the view needs to be recycled for reuse, clear any held references
     * to objects, stop any asynchronous work, e.t.c
     */
    override fun unbind(view: View) {
        binding = null
    }

    companion object {
        internal fun LayoutInflater.createViewHolder(
            viewGroup: ViewGroup
        ) = MarketPlaceCategoryItemBinding.inflate(
            this, viewGroup, false
        ).let { SupportViewHolder(it.root) }
    }
}