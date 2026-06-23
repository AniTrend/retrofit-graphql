package co.anitrend.retrofit.graphql.domain.repositories

import co.anitrend.arch.domain.state.UiState

interface UploadRepository<D: UiState<*>> {
    fun uploadToBucket(path: String): D
}
