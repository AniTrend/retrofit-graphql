package io.github.wax911.library.converter.request

import com.google.gson.Gson
import io.github.wax911.library.annotation.processor.contract.AbstractGraphProcessor
import io.github.wax911.library.model.request.QueryContainerBuilder
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Converter

/**
 * GraphQL request body converter and injector, uses method annotation for a given retrofit method
 */
open class GraphRequestConverter(
    protected val methodAnnotations: Array<out Annotation>,
    protected val graphProcessor: AbstractGraphProcessor,
    protected val gson: Gson
) : Converter<QueryContainerBuilder, RequestBody> {

    /**
     * Converter for the request body, gets the GraphQL query from the method annotation
     * and constructs a GraphQL request body to send over the network.
     *
     * @param containerBuilder The constructed builder method of your query with variables
     */
    override fun convert(containerBuilder: QueryContainerBuilder): RequestBody {
        val rawQuery = graphProcessor.getQuery(methodAnnotations)
        val queryContainer =
            containerBuilder.apply {
                if (containerBuilder.isQueryNullOrEmpty())
                    setQuery(rawQuery)
            }
                .build()
        val queryJson = gson.toJson(queryContainer)
        return RequestBody.create(MEDIA_TYPE, queryJson)
    }

    companion object {
        private val MEDIA_TYPE = MediaType.parse("application/json")
    }
}