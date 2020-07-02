package co.anitrend.retrofit.graphql.domain.usecases

import co.anitrend.arch.domain.common.IUseCase
import co.anitrend.arch.domain.common.IUserInterfaceState
import co.anitrend.retrofit.graphql.domain.repositories.BucketRepository

abstract class BucketUseCase<R: IUserInterfaceState<*>>(
    protected val repository: BucketRepository<R>
) : IUseCase {
    operator fun invoke() =
        repository.getAllFiles()
}