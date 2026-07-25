package co.anitrend.retrofit.graphql.data.bucket.model

import co.anitrend.retrofit.graphql.data.bucket.model.node.BucketFileNode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class StorageBucket(
    @SerialName("files") val files: List<BucketFileNode>
)