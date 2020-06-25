package co.anitrend.retrofit.graphql.data.bucket.model.node

import co.anitrend.retrofit.graphql.data.graphql.common.INode
import com.google.gson.annotations.SerializedName

internal data class BucketFileNode(
    @SerializedName("id") override val id: String,
    @SerializedName("contentType") val contentType: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("url") val url: String
) : INode