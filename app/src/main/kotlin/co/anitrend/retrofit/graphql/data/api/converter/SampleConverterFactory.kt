package co.anitrend.retrofit.graphql.data.api.converter

import android.content.Context
import co.anitrend.retrofit.graphql.data.api.converter.request.SampleRequestConverter
import co.anitrend.retrofit.graphql.data.arch.JSON
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.wax911.library.converter.GraphConverter
import io.github.wax911.library.logger.contract.ILogger
import io.github.wax911.library.util.LogLevel
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type

internal class SampleConverterFactory(
    context: Context
) : GraphConverter(context = context, gson = GSON) {

    /**
     * Response body converter delegates logic processing to a child class that handles
     * wrapping and deserialization of the json response data.
     *
     * @param parameterAnnotations All the annotation applied to request parameters
     * @param methodAnnotations All the annotation applied to the requesting method
     * @param retrofit The retrofit object representing the response
     * @param type The type of the parameter of the request
     *
     * @see GraphRequestConverter
     */
    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody>? {
        return SampleRequestConverter(
            methodAnnotations = methodAnnotations,
            graphProcessor = graphProcessor,
            gson = gson
        )
    }

    /**
     * Response body converter delegates logic processing to a child class that handles
     * wrapping and deserialization of the json response data.
     *
     *
     * @param annotations All the annotation applied to the requesting Call method
     * @param retrofit The retrofit object representing the response
     * @param type The generic type declared on the Call method
     *
     * @see GraphResponseConverter
     */
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        // not going to override the response body converter in this sample
        return super.responseBodyConverter(type, annotations, retrofit)
    }

    companion object {
        private val GSON = GsonBuilder()
            .setLenient()
            .create()

        /**
         * Allows you to provide your own Gson configuration which will be used when serialize or
         * deserialize response and request bodies.
         *
         * @param context any valid application context
         */
        fun create(context: Context) =
            SampleConverterFactory(context).apply {
                setMinimumLogLevel(ILogger.Level.ERROR)
            }
    }
}