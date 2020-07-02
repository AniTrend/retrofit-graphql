package co.anitrend.retrofit.graphql.data.arch

/**
 * Example annotation to use as an indicator that a method supports file uploads
 *
 * @see co.anitrend.retrofit.graphql.data.bucket.helper.UploadMutationHelper
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
internal annotation class GraphMultiPartUpload