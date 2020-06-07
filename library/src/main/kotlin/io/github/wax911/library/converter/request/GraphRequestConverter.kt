package io.github.wax911.library.converter.request

import com.google.gson.Gson
import io.github.wax911.library.annotation.processor.GraphProcessor
import io.github.wax911.library.converter.GraphConverter
import io.github.wax911.library.model.request.QueryContainerBuilder
import io.github.wax911.library.util.Logger
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Converter

/**
 * GraphQL request body converter and injector, uses method annotation for a given retrofit call
 */
open class GraphRequestConverter(
        protected val methodAnnotations: Array<Annotation>,
        protected val graphProcessor: GraphProcessor,
        protected val gson: Gson
) : Converter<QueryContainerBuilder, RequestBody> {

    /**
     * Converter for the request body, gets the GraphQL query from the method annotation
     * and constructs a GraphQL request body to send over the network.
     * <br></br>
     *
     * @param containerBuilder The constructed builder method of your query with variables
     * @return Request body
     */
    override fun convert(containerBuilder: QueryContainerBuilder): RequestBody {
        val queryContainer = containerBuilder
                .setQuery(graphProcessor.getQuery(methodAnnotations))
                .build()
        val queryJson = gson.toJson(queryContainer)
        Logger.d("GraphRequestConverter", queryJson)
        return RequestBody.create(MediaType.parse(GraphConverter.MimeType), queryJson)
    }
}