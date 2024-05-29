package co.anitrend.retrofit.graphql.data.user.usecase

import co.anitrend.arch.data.repository.contract.ISupportRepository
import co.anitrend.arch.data.state.DataState
import co.anitrend.retrofit.graphql.data.user.repository.UserRepositoryContract
import co.anitrend.retrofit.graphql.domain.entities.user.User
import co.anitrend.retrofit.graphql.domain.usecases.UserUseCase

internal class UserUseCaseImpl(
    repository: UserRepositoryContract
) : UserUseCaseContract(repository) {
    /**
     * Informs underlying repositories or related components running background operations to stop
     */
    override fun onCleared() {
        repository as ISupportRepository
        repository.onCleared()
    }
}

typealias UserUseCaseContract = UserUseCase<DataState<User>>