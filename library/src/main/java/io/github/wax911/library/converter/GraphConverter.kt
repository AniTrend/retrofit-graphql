package io.github.wax911.library.converter

import android.content.Context
import android.util.Log
import com.google.gson.ExclusionStrategy

import com.google.gson.Gson
import com.google.gson.GsonBuilder

import java.io.IOException
import java.lang.reflect.Type

import io.github.wax911.library.annotation.processor.GraphProcessor
import io.github.wax911.library.model.request.QueryContainer
import io.github.wax911.library.model.request.QueryContainerBuilder
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit

/**
 * Created by max on 2017/10/22.
 * Body for GraphQL requests and responses, closed for modification
 * but open for extension.
 *
 * Protected constructor because we want to make use of the
 * Factory Pattern to create our converter
 * </br></br>
 *
 * @param context Any valid application context
*/

open class GraphConverter protected constructor(context: Context?) : Converter.Factory() {

    protected val graphProcessor: GraphProcessor? by lazy {
        GraphProcessor.getInstance(context?.assets)
    }

    protected val gson by lazy {
        GsonBuilder()
                .enableComplexMapKeySerialization()
                .serializeNulls()
                .setLenient()
                .create()
    }

    /**
     * Response body converter delegates logic processing to a child class that handles
     * wrapping and deserialization of the json response data.
     * @see GraphResponseConverter
     * <br></br>
     *
     *
     * @param annotations All the annotation applied to the requesting Call method
     * @see retrofit2.Call
     *
     * @param retrofit The retrofit object representing the response
     * @param type The generic type declared on the Call method
     */
    override fun responseBodyConverter(type: Type?, annotations: Array<Annotation>, retrofit: Retrofit): Converter<ResponseBody, *>? {
        return when (type) {
            is ResponseBody -> super.responseBodyConverter(type, annotations, retrofit)
            else -> GraphResponseConverter<Any>(type)
        }
    }

    /**
     * Response body converter delegates logic processing to a child class that handles
     * wrapping and deserialization of the json response data.
     * @see GraphRequestConverter
     * <br></br>
     *
     *
     * @param parameterAnnotations All the annotation applied to request parameters
     * @param methodAnnotations All the annotation applied to the requesting method
     * @param retrofit The retrofit object representing the response
     * @param type The type of the parameter of the request
     */
    override fun requestBodyConverter(
            type: Type?, parameterAnnotations: Array<Annotation>,
            methodAnnotations: Array<Annotation>, retrofit: Retrofit?
    ): Converter<QueryContainerBuilder, RequestBody>? = GraphRequestConverter(methodAnnotations)


    /**
     * GraphQL response body converter to unwrap nested object results,
     * resulting in a smaller generic tree for requests
     */
    open inner class GraphResponseConverter<T>(protected var type: Type?) : Converter<ResponseBody, T> {

        /**
         * Converter contains logic on how to handle responses, since GraphQL responses follow
         * the JsonAPI spec it makes sense to wrap our base query response data and errors response
         * in here, the logic remains open to the implementation
         * <br></br>
         *
         * @param responseBody The retrofit response body received from the network
         * @return The type declared in the Call of the request
         */
        override fun convert(responseBody: ResponseBody): T? {
            var response: T? = null
            try {
                val responseString = responseBody.string()
                response = gson.fromJson<T>(responseString, type)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return response
        }
    }

    /**
     * GraphQL request body converter and injector, uses method annotation for a given retrofit call
     */
    open inner class GraphRequestConverter(protected var methodAnnotations: Array<Annotation>) : Converter<QueryContainerBuilder, RequestBody> {

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
                    .setQuery(graphProcessor?.getQuery(methodAnnotations))
                    .build()
            val queryJson = gson.toJson(queryContainer)
            Log.d("GraphRequestConverter", queryJson)
            return RequestBody.create(MediaType.parse(GraphConverter.MimeType), queryJson)
        }
    }

    companion object {

        const val MimeType = "application/graphql"

        fun create(context: Context): GraphConverter {
            return GraphConverter(context)
        }
    }
}
