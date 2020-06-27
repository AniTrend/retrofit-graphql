package co.anitrend.retrofit.graphql.data.api.converter

import co.anitrend.retrofit.graphql.data.api.converter.request.SampleRequestConverter
import com.google.gson.GsonBuilder
import io.github.wax911.library.annotation.processor.contract.AbstractGraphProcessor
import io.github.wax911.library.converter.GraphConverter
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

/**
 * Allows you to provide your own Gson configuration which will be used when serialize or
 * deserialize response and request bodies.
 *
 * @param processor our managed graphql processor, this is created in
 * [coreModule](co.anitrend.retrofit.graphql.data.arch.koin.coreModule)
 *
 * @see co.anitrend.retrofit.graphql.data.arch.koin.coreModule
 */
internal class SampleConverterFactory(
    processor: AbstractGraphProcessor
) : GraphConverter(
    processor,
    GsonBuilder()
        .setLenient()
        .create()
) {

    /**
     * Response body converter delegates logic processing to a child class that handles
     * wrapping and deserialization of the json response data.
     *
     * @param parameterAnnotations All the annotation applied to request parameters
     * @param methodAnnotations All the annotation applied to the requesting method
     * @param retrofit The retrofit object representing the response
     * @param type The type of the parameter of the request
     *
     * @see SampleRequestConverter
     */
    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody>? {
        return SampleRequestConverter(
            annotations = methodAnnotations,
            processor = graphProcessor,
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
}