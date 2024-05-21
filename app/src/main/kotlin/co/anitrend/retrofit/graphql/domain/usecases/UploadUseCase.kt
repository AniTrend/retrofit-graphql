package co.anitrend.retrofit.graphql.domain.usecases

import co.anitrend.arch.domain.common.IUseCase
import co.anitrend.arch.domain.state.UiState
import co.anitrend.retrofit.graphql.domain.models.common.IGraphQuery
import co.anitrend.retrofit.graphql.domain.repositories.UploadRepository

abstract class UploadUseCase<R: UiState<*>>(
    protected val repository: UploadRepository<R>
) : IUseCase {
    operator fun invoke(mutation: IGraphQuery) =
        repository.uploadToBucket(mutation)
}