package co.anitrend.retrofit.graphql.sample.view.content.bucket.ui.controller.helper

import androidx.recyclerview.widget.DiffUtil
import co.anitrend.retrofit.graphql.domain.entities.bucket.BucketFile

internal val DIFFER =
    object : DiffUtil.ItemCallback<BucketFile>() {
        override fun areItemsTheSame(
            oldItem: BucketFile,
            newItem: BucketFile
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: BucketFile,
            newItem: BucketFile
        ): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }