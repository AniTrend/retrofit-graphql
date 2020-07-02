package co.anitrend.retrofit.graphql.data.bucket.helper

import co.anitrend.retrofit.graphql.data.api.converter.request.SampleRequestConverter
import co.anitrend.retrofit.graphql.data.arch.GraphMultiPartUpload
import co.anitrend.retrofit.graphql.data.bucket.datasource.remote.BucketRemoteSource
import co.anitrend.retrofit.graphql.data.bucket.model.upload.mutation.UploadMutation
import com.google.gson.Gson
import io.github.wax911.library.model.request.QueryContainerBuilder
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * Mutation helper for crating upload request bodies
 */
internal object UploadMutationHelper {

    /** What our example server expects as the mutation key for its multi-part */
    private const val PART_BODY_NAME = "query"
    private const val PART_BODY_OPERATIONS = "operations"
    private const val PART_BODY_MAP = "map"

    private const val PART_FILE_NAME = "upload_file"
    /** The file that is reserved for upload will always be a webp image */
    private val PART_FILE_MIME_TYPE = "image/webp".toMediaTypeOrNull()

    /**
     * A simple way to check if our [QueryContainerBuilder] contains a file,
     * by checking the existence of a key
     *
     * @see UploadMutation
     */
    fun QueryContainerBuilder.containsImage() = containsKey(UploadMutation.KEY)

    /**
     * Another way to check if we the current request method should handle file uploads,
     * by checking if is annotated with [GraphMultiPartUpload]
     */
    fun Array<out Annotation>.supportsFileUpload(): Boolean {
        return filterIsInstance(GraphMultiPartUpload::class.java).isNotEmpty()
    }

    @Throws(Throwable::class)
    private fun createFileBodyPart(path: String): MultipartBody.Part {
        /**
         * [UploadMutation] provides a path to a file which needs to be uploaded.
         * we will resolve the path and throw if we cannot find it
         */
        val uploadFile = File(path)
        if (!uploadFile.exists()) throw Throwable(
            "`UploadMutation#upload` does not point to an existing file -> $path"
        )
        val attachment = uploadFile.asRequestBody(PART_FILE_MIME_TYPE)
        return MultipartBody.Part.createFormData(
            PART_FILE_NAME, uploadFile.name, attachment
        )
    }

    private fun QueryContainerBuilder.createVariablesPart(
        gson: Gson,
        graphQuery: String?,
        graphQueryMediaType: MediaType?
    ): MultipartBody.Part {
        /**
         * Overwrite our initial file key corresponding to [UploadMutation.upload]
         * with [PART_FILE_NAME] which is our multi-part name field for the file.
         *
         * Prior to this [UploadMutation.upload] have a file path to where the image is located
         */
        putVariable(UploadMutation.KEY, PART_FILE_NAME)

        val queryContainer = setQuery(graphQuery).build()
        val queryJson = gson.toJson(queryContainer)
        val requestBody = queryJson.toRequestBody(graphQueryMediaType)
        return MultipartBody.Part.createFormData(
            PART_BODY_OPERATIONS, null, requestBody
        )
    }

    private fun MediaType?.createMapPart(gson: Gson, key: String): MultipartBody.Part {
        /** Add map of variables */
        val mapParts = mapOf(
            PART_FILE_NAME to listOf("variables.$key")
        )
        val queryJson = gson.toJson(mapParts)
        val requestBody = queryJson.toRequestBody(this)
        return MultipartBody.Part.createFormData(
            PART_BODY_MAP, null, requestBody
        )
    }

    /**
     * Creates a multipart-body of a file, mutation and map which follows the
     * [graphql multipart spec](https://github.com/jaydenseric/graphql-multipart-request-spec)
     *
     * @param gson Configured converter, this could be anything from **moshi** to **kotlinx.serializer**
     * @param graphQuery The mutation query that is annotated on your interface method, e.g. [BucketRemoteSource.uploadToStorageBucket]
     * @param graphQueryMediaType The mime type for the [graphQuery] in this case `application/json`
     */
    fun QueryContainerBuilder.createMultiPartBody(
        gson: Gson,
        graphQuery: String?,
        graphQueryMediaType: MediaType?
    ): MultipartBody {
        /**
         * We've already established that the key exists from [containsImage] called
         * in [SampleRequestConverter.convert] so we cast the type to a non-nullable [String]
         *
         * @see [co.anitrend.retrofit.graphql.sample.presenter.BucketPresenter]
         */
        val path = getVariable(UploadMutation.KEY) as String
        /** get multi-part for file */
        val uploadBodyPart = createFileBodyPart(path)
        /** get multi-part for operations */
        val queryBodyPart = createVariablesPart(gson, graphQuery, graphQueryMediaType)
        /** get multi-part for map */
        val mapBodyPart = graphQueryMediaType.createMapPart(gson, UploadMutation.KEY)

        /**
         * Instead of returning **queryBody** we are going to construct our multi-part body
         * here: **N.B** How your graphql server implements what body type to support for the query
         * may differ from this example, but the concept should be the same
         */
        return MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addPart(queryBodyPart)
            .addPart(mapBodyPart)
            .addPart(uploadBodyPart)
            .build()
    }

    /**
     *  Another example that creates a multipart-body of a file and mutation which
     *  may not support the [graphql multi-part spec](https://github.com/jaydenseric/graphql-multipart-request-spec)
     */
    fun QueryContainerBuilder.createMultiPartBody(): MultipartBody {
        val path = getVariable(UploadMutation.KEY) as String
        val uploadBodyPart = createFileBodyPart(path)

        /**
         * Unlike the above example we making use of plain mutation without any variables.
         *
         * This could be a graphql file defined as follows with a keyword you'd replace upon
         * inspecting what the output of [io.github.wax911.library.annotation.processor.contract.AbstractGraphProcessor.getQuery]
         */
        val plainMutation = """
            mutation { uploadFile(fileData: "$PART_FILE_NAME") { contentType filename id url }}
        """.trimIndent()

        return MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(PART_BODY_NAME, plainMutation)
            .addPart(uploadBodyPart)
            .build()
    }
}