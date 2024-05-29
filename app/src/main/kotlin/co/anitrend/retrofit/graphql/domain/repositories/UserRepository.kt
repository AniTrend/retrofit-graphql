package co.anitrend.retrofit.graphql.domain.repositories

import co.anitrend.arch.domain.state.UiState

interface UserRepository<D: UiState<*>> {
    fun getCurrentUser(): D
}