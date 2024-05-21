package co.anitrend.retrofit.graphql.sample.view.content.bucket.ui.controller.model

import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import co.anitrend.arch.recycler.action.contract.ISupportSelectionMode
import co.anitrend.arch.recycler.common.ClickableItem
import co.anitrend.arch.recycler.holder.SupportViewHolder
import co.anitrend.arch.recycler.model.RecyclerItem
import co.anitrend.retrofit.graphql.core.extension.using
import co.anitrend.retrofit.graphql.domain.entities.bucket.BucketFile
import co.anitrend.retrofit.graphql.sample.R
import co.anitrend.retrofit.graphql.sample.databinding.BucketFileItemBinding
import coil.request.Disposable
import coil.transform.RoundedCornersTransformation
import kotlinx.coroutines.flow.MutableStateFlow

internal class BucketFileItem(
    private val entity: BucketFile?
) : RecyclerItem(entity?.id?.toLong()) {

    private var disposable: Disposable? = null
    private var binding: BucketFileItemBinding? = null

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
        binding = BucketFileItemBinding.bind(view)
        binding?.bucketImageName?.text = entity?.fileName
        val margin = view.resources.getDimension(co.anitrend.arch.ui.R.dimen.lg_margin)
        binding?.bucketImage?.using(
            entity?.url,
            ColorDrawable(-0x333334),
            RoundedCornersTransformation(
                margin, margin, margin, margin
            )
        )
    }

    /**
     * Provides a preferred span size for the item
     *
     * @param spanCount current span count which may also be [INVALID_SPAN_COUNT]
     * @param position position of the current item
     * @param resources optionally useful for dynamic size check with different configurations
     */
    override fun getSpanSize(spanCount: Int, position: Int, resources: Resources): Int {
        return resources.getInteger(co.anitrend.arch.ui.R.integer.grid_list_x3)
    }

    /**
     * Called when the view needs to be recycled for reuse, clear any held references
     * to objects, stop any asynchronous work, e.t.c
     */
    override fun unbind(view: View) {
        disposable?.dispose()
        disposable = null
        binding = null
    }

    companion object {
        internal fun LayoutInflater.createViewHolder(
            viewGroup: ViewGroup
        ) = BucketFileItemBinding.inflate(
            this, viewGroup, false
        ).let { SupportViewHolder(it.root) }
    }
}