package co.anitrend.retrofit.graphql.domain.usecases

import co.anitrend.arch.domain.common.IUseCase
import co.anitrend.arch.domain.state.UiState
import co.anitrend.retrofit.graphql.domain.repositories.UserRepository

abstract class UserUseCase<R: UiState<*>>(
    protected val repository: UserRepository<R>
) : IUseCase {
    operator fun invoke() =
        repository.getCurrentUser()
}