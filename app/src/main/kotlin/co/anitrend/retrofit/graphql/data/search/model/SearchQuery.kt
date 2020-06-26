package co.anitrend.retrofit.graphql.data.search.model

import co.anitrend.retrofit.graphql.domain.models.common.IGraphQuery
import co.anitrend.retrofit.graphql.domain.models.enums.SearchType

data class SearchQuery(
    val query: String,
    val type: SearchType
) : IGraphQuery {
    override fun toMap() = mapOf(
        "query" to query,
        "type" to type.name
    )
}