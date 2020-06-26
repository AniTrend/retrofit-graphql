package co.anitrend.retrofit.graphql.data.bucket.model.upload

import co.anitrend.retrofit.graphql.data.bucket.model.node.BucketFileNode
import com.google.gson.annotations.SerializedName

internal data class UploadResult(
    @SerializedName("uploadFile") val uploaded: BucketFileNode
)