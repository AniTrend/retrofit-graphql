package co.anitrend.retrofit.graphql.domain.models.common

/**
 * Contract type of a query
 */
interface IGraphQuery {
    fun toMap(): Map<String, Any?>
}