package co.anitrend.retrofit.graphql.data.bucket.model.upload

import co.anitrend.retrofit.graphql.data.bucket.model.node.BucketFileNode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class UploadResult(
    @SerialName("uploadFile") val uploaded: BucketFileNode
)