package co.anitrend.retrofit.graphql.data.arch.mapper

import co.anitrend.arch.data.mapper.SupportResponseMapper
import io.github.wax911.library.model.body.GraphContainer
import timber.log.Timber

/**
 * GraphQLMapper specific mapper, assures that all requests respond with [GraphContainer]
 * as the root tree object.
 *
 * Making it easier for us to implement error logging and provide better error messages
 */
internal abstract class GraphQLMapper<S, D> : SupportResponseMapper<S, D>() {

    /**
     * Simple logger for empty response
     */
    protected fun onEmptyResponse() {
        Timber.tag(moduleTag).v(
            "onResponseDatabaseInsert -> mappedData is empty"
        )
    }
}