package co.anitrend.retrofit.graphql.domain.usecases

import co.anitrend.arch.domain.common.IUseCase
import co.anitrend.arch.domain.state.UiState
import co.anitrend.retrofit.graphql.domain.repositories.UploadRepository

abstract class UploadUseCase<R: UiState<*>>(
    protected val repository: UploadRepository<R>
) : IUseCase {
    operator fun invoke(path: String) =
        repository.uploadToBucket(path)
}
