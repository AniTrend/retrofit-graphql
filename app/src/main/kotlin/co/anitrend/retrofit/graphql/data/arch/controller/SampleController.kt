package co.anitrend.retrofit.graphql.data.arch.controller

import androidx.lifecycle.MutableLiveData
import androidx.paging.PagingRequestHelper
import co.anitrend.arch.data.common.ISupportPagingResponse
import co.anitrend.arch.data.common.ISupportResponse
import co.anitrend.arch.data.mapper.SupportResponseMapper
import co.anitrend.arch.domain.entities.NetworkState
import co.anitrend.arch.extension.dispatchers.SupportDispatchers
import co.anitrend.retrofit.graphql.data.arch.controller.strategy.ControllerStrategy
import co.anitrend.retrofit.graphql.data.arch.extensions.fetchBodyWithRetry
import io.github.wax911.library.model.attribute.GraphError
import io.github.wax911.library.model.body.GraphContainer
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withContext
import retrofit2.Response

internal class SampleController<S, D> private constructor(
    private val responseMapper: SupportResponseMapper<S, D>,
    private val strategy: ControllerStrategy<D>,
    private val dispatchers: SupportDispatchers
) : ISupportResponse<Deferred<Response<GraphContainer<S>>>, D>,
    ISupportPagingResponse<Deferred<Response<GraphContainer<S>>>> {

    @Throws
    private fun handleErrorsIfExist(errors: List<GraphError>) {
        if (errors.isNotEmpty())
            throw Throwable(
                message = errors.joinToString { "${it.message} ${it.locations}" },
                cause = Throwable("GraphQL endpoint returned error/s")
            )
    }

    /**
     * Response handler for coroutine contexts which need to observe [NetworkState]
     *
     * @param resource awaiting execution
     * @param networkState for the deferred result
     *
     * @return resource fetched if present
     */
    override suspend fun invoke(
        resource: Deferred<Response<GraphContainer<S>>>,
        networkState: MutableLiveData<NetworkState>
    ): D? {
        return strategy({
            val responseBody = resource.fetchBodyWithRetry(dispatchers.io)
            val mapped = withContext(dispatchers.computation) {
                handleErrorsIfExist(responseBody.errors.orEmpty())
                responseBody.data?.let { data ->
                    responseMapper.onResponseMapFrom(data)
                }
            }
            withContext(dispatchers.io) {
                if (mapped != null)
                    responseMapper.onResponseDatabaseInsert(mapped)
            }
            mapped
        }, networkState)
    }

    /**
     * Response handler for coroutine contexts, mainly for paging
     *
     * @param resource awaiting execution
     * @param pagingRequestHelper optional paging request callback
     */
    override suspend fun invoke(
        resource: Deferred<Response<GraphContainer<S>>>,
        pagingRequestHelper: PagingRequestHelper.Request.Callback
    ) {
        strategy({
            val responseBody = resource.fetchBodyWithRetry(dispatchers.io)
            val mapped = withContext(dispatchers.computation) {
                handleErrorsIfExist(responseBody.errors.orEmpty())
                responseBody.data?.let { data ->
                    responseMapper.onResponseMapFrom(data)
                }
            }
            withContext(dispatchers.io) {
                if (mapped != null)
                    responseMapper.onResponseDatabaseInsert(mapped)
            }
        }, pagingRequestHelper)
    }


    companion object {
        fun <S, D> newInstance(
            responseMapper: SupportResponseMapper<S, D>,
            strategy: ControllerStrategy<D>,
            supportDispatchers: SupportDispatchers
        ) = SampleController(
            responseMapper,
            strategy,
            supportDispatchers
        )
    }
}