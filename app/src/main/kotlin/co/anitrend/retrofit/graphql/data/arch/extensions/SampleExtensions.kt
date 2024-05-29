package co.anitrend.retrofit.graphql.data.arch.extensions

import co.anitrend.arch.data.mapper.SupportResponseMapper
import co.anitrend.arch.extension.dispatchers.contract.ISupportDispatcher
import co.anitrend.retrofit.graphql.data.arch.controller.SampleController
import co.anitrend.retrofit.graphql.data.arch.controller.policy.OfflineControllerPolicy
import co.anitrend.retrofit.graphql.data.arch.controller.policy.OnlineControllerPolicy
import co.anitrend.retrofit.graphql.data.arch.controller.strategy.ControllerStrategy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.core.scope.Scope
import retrofit2.HttpException
import retrofit2.Response
import java.io.FileNotFoundException
import java.io.IOException
import java.net.SocketTimeoutException

internal fun <T> Response<T>.bodyOrThrow(): T {
    if (!isSuccessful) throw HttpException(this)
    return body()!!
}

private suspend inline fun <T> Deferred<Response<T>>.executeWithRetry(
    dispatcher: CoroutineDispatcher,
    defaultDelay: Long = 100,
    maxAttempts: Int = 3,
    shouldRetry: (Exception) -> Boolean = ::defaultShouldRetry
): Response<T> {
    repeat(maxAttempts) { attempt ->
        var nextDelay = attempt * attempt * defaultDelay
        try {
            return withContext(dispatcher) { await() }
        } catch (e: Exception) {
            // The response failed, so lets see if we should retry again
            if (attempt == (maxAttempts - 1) || !shouldRetry(e)) {
                throw e
            }

            if (e is HttpException) {
                // If we have a HttpException, check whether we have a Retry-After
                // header to decide how long to delay
                val retryAfterHeader = e.response()?.headers()?.get("Retry-After")
                if (!retryAfterHeader.isNullOrEmpty()) {
                    // Got a Retry-After value, try and parse it to an long
                    try {
                        nextDelay = (retryAfterHeader.toLong() + 10).coerceAtLeast(defaultDelay)
                    } catch (nfe: NumberFormatException) {
                        // Probably won't happen, ignore the value and use the generated default above
                    }
                }
            }
        }

        delay(nextDelay)
    }

    // We should never hit here
    throw IllegalStateException("Unknown exception from executeWithRetry")
}

internal suspend inline fun <T> Deferred<Response<T>>.fetchBodyWithRetry(
    dispatcher: CoroutineDispatcher,
    firstDelay: Long = 100,
    maxAttempts: Int = 3,
    shouldRetry: (Exception) -> Boolean = ::defaultShouldRetry
) = executeWithRetry(dispatcher, firstDelay, maxAttempts, shouldRetry).bodyOrThrow()

private fun defaultShouldRetry(exception: Exception) = when (exception) {
    is HttpException -> exception.code() == 429
    is FileNotFoundException -> true
    is SocketTimeoutException -> true
    is IOException -> true
    else -> false
}

/**
 * Extension to help us create a controller from a a mapper instance
 */
internal fun <S, D> SupportResponseMapper<S, D>.controller(
    strategy: ControllerStrategy<D>,
    supportDispatchers: ISupportDispatcher
) = SampleController.newInstance(
    responseMapper = this,
    strategy = strategy,
    dispatcher = supportDispatchers.io
)

internal fun <T> Scope.onlineController() =
    OnlineControllerPolicy.create<T>(get())

internal fun <T> offlineController() =
    OfflineControllerPolicy.create<T>()