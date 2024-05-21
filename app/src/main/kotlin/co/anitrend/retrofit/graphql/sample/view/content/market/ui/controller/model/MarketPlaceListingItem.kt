package co.anitrend.retrofit.graphql.sample.view.content.market.ui.controller.model

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import co.anitrend.arch.extension.ext.getCompatDrawable
import co.anitrend.arch.extension.ext.gone
import co.anitrend.arch.extension.ext.visible
import co.anitrend.arch.recycler.action.contract.ISupportSelectionMode
import co.anitrend.arch.recycler.common.ClickableItem
import co.anitrend.arch.recycler.common.DefaultClickableItem
import co.anitrend.arch.recycler.holder.SupportViewHolder
import co.anitrend.arch.recycler.model.RecyclerItem
import co.anitrend.arch.ui.extension.setUpWith
import co.anitrend.retrofit.graphql.core.extension.using
import co.anitrend.retrofit.graphql.domain.entities.market.MarketPlaceListing
import co.anitrend.retrofit.graphql.sample.R
import co.anitrend.retrofit.graphql.sample.databinding.MarketPlaceItemBinding
import co.anitrend.retrofit.graphql.sample.view.content.market.ui.adapter.MarketPlaceCategoryAdapter
import coil.request.Disposable
import kotlinx.coroutines.flow.MutableStateFlow

class MarketPlaceListingItem(
    private val entity: MarketPlaceListing?
) : RecyclerItem(entity?.id?.hashCode()?.toLong()) {

    private var disposable: Disposable? = null
    private var binding: MarketPlaceItemBinding? = null

    private fun setUpCategories() {
        binding?.apply {
            val adapter = MarketPlaceCategoryAdapter(resources = root.resources)
            listingCategories.setUpWith(
                supportAdapter = adapter,
                recyclerLayoutManager = LinearLayoutManager(
                    root.context, LinearLayoutManager.HORIZONTAL, false
                )
            )

            adapter.submitList(entity?.categories as List)
        }
    }

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
    override fun bind(
        view: View,
        position: Int,
        payloads: List<Any>,
        stateFlow: MutableStateFlow<ClickableItem?>,
        selectionMode: ISupportSelectionMode<Long>?
    ) {
        if (entity == null) return
        binding = MarketPlaceItemBinding.bind(view)
        binding?.listingContainer?.setOnClickListener {
            stateFlow.value = DefaultClickableItem(
                data = entity,
                view = view
            )
        }
        disposable = binding?.listingImage?.using(entity.logoUrl)
        binding?.listingName?.text = entity.name
        binding?.listingDescription?.text = entity.description
        if (entity.isVerified) {
            binding?.listingVerification?.visible()
            binding?.listingVerification?.setImageDrawable(
                view.context.getCompatDrawable(
                    R.drawable.ic_whatshot_24dp,
                    co.anitrend.arch.ui.R.color.colorStateBlue
                )
            )
        }
        else
            binding?.listingVerification?.gone()
        setUpCategories()
    }

    /**
     * Provides a preferred span size for the item
     *
     * @param spanCount current span count which may also be [INVALID_SPAN_COUNT]
     * @param position position of the current item
     * @param resources optionally useful for dynamic size check with different configurations
     */
    override fun getSpanSize(spanCount: Int, position: Int, resources: Resources): Int {
        return resources.getInteger(co.anitrend.arch.ui.R.integer.single_list_size)
    }

    /**
     * Called when the view needs to be recycled for reuse, clear any held references
     * to objects, stop any asynchronous work, e.t.c
     */
    override fun unbind(view: View) {
        disposable?.dispose()
        disposable = null
        binding?.listingContainer?.setOnClickListener(null)
        binding = null
    }

    companion object {
        internal fun LayoutInflater.createViewHolder(
            viewGroup: ViewGroup
        ) = MarketPlaceItemBinding.inflate(
            this, viewGroup, false
        ).let { SupportViewHolder(it.root) }
    }
}