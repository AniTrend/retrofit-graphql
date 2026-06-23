package co.anitrend.retrofit.graphql.data.api.converter

import okhttp3.RequestBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

/**
 * Preserves explicit [RequestBody] instances so Retrofit does not try to
 * re-serialize multipart or other prebuilt request payloads.
 */
internal object RequestBodyPassThroughConverterFactory : Converter.Factory() {
    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<*, RequestBody>? {
        if (!RequestBody::class.java.isAssignableFrom(getRawType(type))) {
            return null
        }

        return Converter<Any, RequestBody> { value ->
            value as RequestBody
        }
    }
}
