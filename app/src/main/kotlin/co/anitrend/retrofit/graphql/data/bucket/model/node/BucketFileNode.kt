package co.anitrend.retrofit.graphql.data.bucket.model.node

import co.anitrend.retrofit.graphql.data.graphql.common.INode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class BucketFileNode(
    @SerialName("id") override val id: String,
    @SerialName("contentType") val contentType: String,
    @SerialName("filename") val filename: String,
    @SerialName("url") val url: String
) : INode