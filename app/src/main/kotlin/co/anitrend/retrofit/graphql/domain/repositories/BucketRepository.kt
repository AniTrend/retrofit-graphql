package co.anitrend.retrofit.graphql.domain.repositories

import co.anitrend.arch.domain.state.UiState

interface BucketRepository<D: UiState<*>> {
    fun getAllFiles() : D
}