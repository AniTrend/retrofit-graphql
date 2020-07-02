package co.anitrend.retrofit.graphql.data.bucket.model

import co.anitrend.retrofit.graphql.data.bucket.model.node.BucketFileNode
import com.google.gson.annotations.SerializedName

internal data class StorageBucket(
    @SerializedName("files") val files: List<BucketFileNode>
)