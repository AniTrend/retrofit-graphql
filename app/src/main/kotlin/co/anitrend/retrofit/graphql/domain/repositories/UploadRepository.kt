package co.anitrend.retrofit.graphql.domain.repositories

import co.anitrend.arch.domain.state.UiState
import co.anitrend.retrofit.graphql.domain.models.common.IGraphQuery

interface UploadRepository<D: UiState<*>> {
    fun uploadToBucket(mutation: IGraphQuery): D
}