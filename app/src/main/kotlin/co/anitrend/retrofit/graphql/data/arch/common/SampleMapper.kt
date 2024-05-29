package co.anitrend.retrofit.graphql.data.arch.common

import co.anitrend.arch.data.mapper.SupportResponseMapper
import co.anitrend.retrofit.graphql.domain.common.EntityId

/**
 * @param E entity
 * @param M model
 */
internal abstract class SampleMapper<E: EntityId, M: Any> {
    protected abstract fun from(): SupportResponseMapper<E, M>
    protected abstract fun to(): SupportResponseMapper<M, E>
}