package co.anitrend.retrofit.graphql.data.bucket.helper

import co.anitrend.retrofit.graphql.model.GraphQLRequest
import co.anitrend.retrofit.graphql.sample.bucket.UploadToStorageBucketVariables
import com.google.gson.Gson
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * Mutation helper for creating GraphQL multipart upload request bodies.
 *
 * Uses Gson directly for serializing the operations and map parts of the
 * multipart body. This is intentionally separate from the Retrofit converter
 * chain which uses kotlinx.serialization. Gson is used here because:
 * - The multipart body format requires constructing JSON from
 *   [Map] and [GraphQLRequest] objects which may contain
 *   [kotlinx.serialization.Transient] fields.
 * - kotlinx.serialization does not support [Map<String, Any?>] types
 *   used in [GraphQLRequest.extensions].
 * - The Gson usage is scoped to multipart uploads only and does not
 *   affect the main request/response serialization pipeline.
 */
internal object UploadMutationHelper {

    private const val PART_BODY_OPERATIONS = "operations"
    private const val PART_BODY_MAP = "map"

    private const val PART_FILE_NAME = "upload_file"
    /** The file that is reserved for upload will always be a webp image */
    private val PART_FILE_MIME_TYPE = "image/webp".toMediaTypeOrNull()
    private val GRAPHQL_MEDIA_TYPE = "application/json".toMediaTypeOrNull()

    @Throws(Throwable::class)
    private fun createFileBodyPart(path: String): MultipartBody.Part {
        /**
         * The provided path to a file which needs to be uploaded.
         * we will resolve the path and throw if we cannot find it
         */
        val uploadFile = File(path)
        if (!uploadFile.exists()) throw Throwable(
            "Upload file does not exist at -> $path"
        )
        val attachment = uploadFile.asRequestBody(PART_FILE_MIME_TYPE)
        return MultipartBody.Part.createFormData(
            PART_FILE_NAME, uploadFile.name, attachment
        )
    }

    private fun GraphQLRequest<UploadToStorageBucketVariables>.createOperationsPart(
        gson: Gson,
        graphQueryMediaType: MediaType?
    ): MultipartBody.Part {
        val queryJson = createOperationsJson(gson)
        val requestBody = queryJson.toRequestBody(graphQueryMediaType)
        return MultipartBody.Part.createFormData(
            PART_BODY_OPERATIONS, null, requestBody
        )
    }

    internal fun GraphQLRequest<UploadToStorageBucketVariables>.createOperationsJsonForReleaseVerification(
        gson: Gson,
    ): String = createOperationsJson(gson)

    private fun GraphQLRequest<UploadToStorageBucketVariables>.createOperationsJson(
        gson: Gson,
    ): String {
        val variables = requireNotNull(variables) {
            "Upload mutation variables are required for multipart GraphQL uploads"
        }
        val operations = copy(variables = variables.copy(upload = PART_FILE_NAME))
        return gson.toJson(operations)
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
     */
    fun GraphQLRequest<UploadToStorageBucketVariables>.createMultiPartBody(
        gson: Gson,
    ): MultipartBody {
        val uploadVariables = requireNotNull(variables) {
            "Upload mutation variables are required for multipart GraphQL uploads"
        }
        val path = uploadVariables.upload
        /** get multi-part for file */
        val uploadBodyPart = createFileBodyPart(path)
        /** get multi-part for operations */
        val queryBodyPart = createOperationsPart(gson, GRAPHQL_MEDIA_TYPE)
        /** get multi-part for map */
        val mapBodyPart = GRAPHQL_MEDIA_TYPE.createMapPart(gson, "upload")

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
}
