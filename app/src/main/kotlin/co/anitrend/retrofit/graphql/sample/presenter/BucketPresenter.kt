package co.anitrend.retrofit.graphql.sample.presenter

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import co.anitrend.arch.core.presenter.SupportPresenter
import co.anitrend.arch.domain.entities.LoadState
import co.anitrend.arch.domain.entities.RequestError
import co.anitrend.retrofit.graphql.core.settings.Settings
import co.anitrend.retrofit.graphql.data.bucket.model.upload.mutation.UploadMutation
import io.wax911.emojify.EmojiManager
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class BucketPresenter(
    context: Context,
    settings: Settings,
    private val emojiManager: EmojiManager,
) : SupportPresenter<Settings>(context, settings) {

    /**
     * Resolves the selected image uri into a [UploadMutation] that contains the absolute
     * path of the selected file
     *
     * @return [UploadMutation] or null
     *
     * @see [co.anitrend.retrofit.graphql.data.bucket.helper.UploadMutationHelper]
     */
    fun resolve(uri: Uri, contentResolver: ContentResolver): UploadMutation? {
        val stream = contentResolver.openInputStream(uri)
        val outputFile = stream?.optimizeImage(context)
        return outputFile?.let { UploadMutation(it.absolutePath) }
    }

    fun loadStateFailure(): LoadState {
        val emoji = emojiManager.getForAlias("gallery")
        return LoadState.Error(
            RequestError(
                topic = "${emoji?.unicode} No images found",
                description = "Try to upload some pictures and they will show up here"
            )
        )
    }

    companion object {
        private fun InputStream.optimizeImage(context: Context): File? {
            val cache = context.externalCacheDir ?: context.cacheDir
            val uploadImage = File(cache, "upload_image.webp")
            if (!uploadImage.exists()) {
                if (!uploadImage.createNewFile()) {
                    Timber
                        .e("Failed to create optimized image for upload")
                    return null
                }
            }

            val outputStream = ByteArrayOutputStream()

            val selectedImage = BitmapFactory.decodeStream(this)
            selectedImage.compress(
                Bitmap.CompressFormat.WEBP,
                100,
                outputStream
            )

            outputStream.flush()
            outputStream.close()

            val fileOutputStream = FileOutputStream(uploadImage)
            fileOutputStream.write(outputStream.toByteArray())

            fileOutputStream.flush()
            fileOutputStream.close()

            selectedImage.recycle()

            return uploadImage
        }
    }
}