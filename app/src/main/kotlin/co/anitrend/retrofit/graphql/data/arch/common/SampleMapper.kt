package co.anitrend.retrofit.graphql.data.arch.common

import co.anitrend.arch.data.mapper.contract.ISupportMapperHelper
import co.anitrend.retrofit.graphql.domain.common.EntityId

/**
 * @param E entity
 * @param M model
 */
internal abstract class SampleMapper<E: EntityId, M: Any> {
    protected abstract fun from(): ISupportMapperHelper<E, M>
    protected abstract fun to(): ISupportMapperHelper<M, E>
}