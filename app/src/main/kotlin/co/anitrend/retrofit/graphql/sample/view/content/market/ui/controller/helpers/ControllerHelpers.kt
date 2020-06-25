package co.anitrend.retrofit.graphql.sample.view.content.market.ui.controller.helpers

import androidx.recyclerview.widget.DiffUtil
import co.anitrend.retrofit.graphql.domain.entities.market.MarketPlaceListing

internal val DIFFER =
    object : DiffUtil.ItemCallback<MarketPlaceListing>() {
        override fun areItemsTheSame(
            oldItem: MarketPlaceListing,
            newItem: MarketPlaceListing
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: MarketPlaceListing,
            newItem: MarketPlaceListing
        ): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

internal val CATEGORY_DIFFER =
    object : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(
            oldItem: String,
            newItem: String
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: String,
            newItem: String
        ): Boolean {
            return oldItem == newItem
        }
    }