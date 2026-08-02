package co.anitrend.retrofit.graphql.data.arch.controller

import co.anitrend.arch.data.common.ISupportResponse
import co.anitrend.arch.data.mapper.SupportResponseMapper
import co.anitrend.arch.request.callback.RequestCallback
import co.anitrend.retrofit.graphql.data.arch.controller.strategy.ControllerStrategy
import co.anitrend.retrofit.graphql.data.arch.extensions.fetchBodyWithRetry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withContext
import retrofit2.Response

internal class SampleController<S, out D> private constructor(
    private val responseMapper: SupportResponseMapper<S, D>,
    private val strategy: ControllerStrategy<D>,
    private val dispatcher: CoroutineDispatcher
) : ISupportResponse<Deferred<Response<SampleEnvelope<S>>>, D> {

    @Throws
    private fun handleErrorsIfExist(errors: List<String>) {
        if (errors.isNotEmpty())
            throw Throwable(
                message = errors.joinToString(),
                cause = Throwable("GraphQL endpoint returned error/s")
            )
    }

    suspend operator fun invoke(
        resource: Deferred<Response<SampleEnvelope<S>>>,
        requestCallback: RequestCallback,
        interceptor: (S) -> S
    ) = strategy(requestCallback) {
        val responseBody = resource.fetchBodyWithRetry(dispatcher)
        val mapped = withContext(dispatcher) {
            handleErrorsIfExist(responseBody.errorMessages)
            responseBody.data?.let {
                val data = interceptor(it)
                responseMapper.onResponseMapFrom(data)
            }
        }
        withContext(dispatcher) {
            if (mapped != null)
                responseMapper.onResponseDatabaseInsert(mapped)
        }
        mapped
    }

    /**
     * Response handler for coroutine contexts which need to observe [LoadState]
     *
     * @param resource awaiting execution
     * @param requestCallback for the deferred result
     *
     * @return resource fetched if present
     */
    override suspend fun invoke(
        resource: Deferred<Response<SampleEnvelope<S>>>,
        requestCallback: RequestCallback
    ) = invoke(resource, requestCallback) { it }

    companion object {
        fun <S, D> newInstance(
            responseMapper: SupportResponseMapper<S, D>,
            strategy: ControllerStrategy<D>,
            dispatcher: CoroutineDispatcher
        ) = SampleController(
            responseMapper,
            strategy,
            dispatcher
        )
    }
}
