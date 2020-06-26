package co.anitrend.retrofit.graphql.domain.entities.bucket

import co.anitrend.retrofit.graphql.domain.common.EntityId

data class BucketFile(
    override val id: String,
    val contentType: String,
    val fileName: String,
    val url: String
) : EntityId