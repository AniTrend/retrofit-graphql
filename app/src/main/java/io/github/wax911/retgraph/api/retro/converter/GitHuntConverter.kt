package io.github.wax911.retgraph.api.retro.converter

import android.content.Context
import com.google.gson.GsonBuilder
import io.github.wax911.library.converter.GraphConverter
import io.github.wax911.library.converter.request.GraphRequestConverter
import io.github.wax911.library.converter.response.GraphResponseConverter
import io.github.wax911.library.model.request.QueryContainerBuilder
import io.github.wax911.retgraph.api.retro.converter.request.RequestConverter
import io.github.wax911.retgraph.api.retro.converter.response.ResponseConverter
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

/**
 * Optionally overriding the GraphConverter to customize it's implementation, otherwise one
 * could just use the default impl which does everything just fine
 */
class GitHuntConverter private constructor(context: Context): GraphConverter(context) {

    companion object {
        fun create(context: Context): GitHuntConverter =
                GitHuntConverter(context).apply {
                    gson = GsonBuilder()
                            .enableComplexMapKeySerialization()
                            .serializeNulls()
                            .setLenient()
                            .create()
                }
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
            else -> ResponseConverter<Any>(type, gson)
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
            type: Type?,
            parameterAnnotations: Array<Annotation>,
            methodAnnotations: Array<Annotation>,
            retrofit: Retrofit?): Converter<QueryContainerBuilder, RequestBody>? {
        return RequestConverter(methodAnnotations, graphProcessor, gson)
    }
}