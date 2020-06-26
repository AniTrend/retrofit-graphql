package co.anitrend.retrofit.graphql.data.bucket.model.upload.mutation

import co.anitrend.retrofit.graphql.domain.models.common.IGraphQuery

data class UploadMutation(
    val upload: String
) : IGraphQuery {
    override fun toMap() =
        mapOf(KEY to upload)

    companion object {
        internal const val KEY = "upload"
    }
}