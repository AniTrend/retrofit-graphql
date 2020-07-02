package co.anitrend.retrofit.graphql.data.api.converter.request

import co.anitrend.retrofit.graphql.data.bucket.helper.UploadMutationHelper
import co.anitrend.retrofit.graphql.data.bucket.helper.UploadMutationHelper.containsImage
import co.anitrend.retrofit.graphql.data.bucket.helper.UploadMutationHelper.createMultiPartBody
import com.google.gson.Gson
import io.github.wax911.library.annotation.processor.contract.AbstractGraphProcessor
import io.github.wax911.library.converter.request.GraphRequestConverter
import io.github.wax911.library.model.request.QueryContainerBuilder
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Request converter that handles how [QueryContainerBuilder] should be converted to
 * a [RequestBody]
 */
internal class SampleRequestConverter(
    annotations: Array<out Annotation>,
    processor: AbstractGraphProcessor,
    gson: Gson
) : GraphRequestConverter(annotations, processor, gson) {

    /**
     * Converter for the request body, gets the GraphQL query from the method annotation
     * and constructs a GraphQL request body to send over the network.
     *
     * @param containerBuilder The constructed builder method of your query with variables
     * @return Request body
     */
    override fun convert(containerBuilder: QueryContainerBuilder): RequestBody {
        val miniQuery = graphProcessor.getQuery(methodAnnotations)
        val mediaType = MIME_TYPE.toMediaTypeOrNull()

        /**
         * Simply checking if [containerBuilder] has a key which we believe to be an image, we
         * can also make use of [UploadMutationHelper.supportsFileUpload] as well e.g.
         * `methodAnnotations.supportsFileUpload()`
         */
        if (containerBuilder.containsImage())
            return containerBuilder.createMultiPartBody(gson, miniQuery, mediaType)

        val queryContainer = containerBuilder.setQuery(miniQuery).build()
        val queryJson = gson.toJson(queryContainer)
        return queryJson.toRequestBody(mediaType)
    }

    companion object {
        internal const val MIME_TYPE = "application/json"
    }
}