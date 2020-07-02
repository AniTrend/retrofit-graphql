package co.anitrend.retrofit.graphql.domain.usecases

import co.anitrend.arch.domain.common.IUseCase
import co.anitrend.arch.domain.common.IUserInterfaceState
import co.anitrend.retrofit.graphql.domain.repositories.UserRepository

abstract class UserUseCase<R: IUserInterfaceState<*>>(
    protected val repository: UserRepository<R>
) : IUseCase {
    operator fun invoke() =
        repository.getCurrentUser()
}