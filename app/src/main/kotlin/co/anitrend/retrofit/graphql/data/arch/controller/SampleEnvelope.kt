package co.anitrend.retrofit.graphql.data.arch.controller

import co.anitrend.retrofit.graphql.model.GraphQLData
import co.anitrend.retrofit.graphql.model.GraphQLResponse
import co.anitrend.retrofit.graphql.model.body.GraphContainer

/**
 * Unified view over the two GraphQL response envelopes used by the sample.
 *
 * The GitHub generated-request path decodes the backend-neutral
 * [GraphQLResponse] contract, while the bucket asset-based demonstration
 * keeps the legacy [GraphContainer] envelope routed through the deprecated
 * [co.anitrend.retrofit.graphql.converter.GraphConverter]. The controller
 * only needs the error messages and the optional data, so both shapes are
 * adapted into this sealed envelope at the source layer.
 *
 * @param S The decoded data type of the envelope.
 */
internal sealed interface SampleEnvelope<out S> {
    /** Human-readable error lines in the same shape the controller used for [GraphContainer]. */
    val errorMessages: List<String>

    /** The decoded data, or null when the response contains only errors. */
    val data: S?

    /**
     * Adapter for the legacy [GraphContainer] envelope of the asset-based
     * bucket flow.
     */
    data class Legacy<S>(
        val container: GraphContainer<S>,
    ) : SampleEnvelope<S> {
        override val errorMessages: List<String>
            get() = container.errors.orEmpty().map { "${it.message} ${it.locations}" }

        override val data: S?
            get() = container.data
    }

    /**
     * Adapter for the backend-neutral [GraphQLResponse] envelope of the
     * generated-request flow. `data: null` and a missing `data` entry both
     * map to a null value here; the distinction is not needed by the UI.
     */
    data class Neutral<S>(
        val response: GraphQLResponse<S>,
    ) : SampleEnvelope<S> {
        override val errorMessages: List<String>
            get() = response.errors.map { "${it.message} ${it.locations}" }

        override val data: S?
            get() = (response.data as? GraphQLData.Present<S>)?.value
    }
}
