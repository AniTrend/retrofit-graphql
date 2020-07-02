package co.anitrend.retrofit.graphql.sample.extensions

import android.content.Context
import co.anitrend.retrofit.graphql.sample.App
import io.wax911.emojify.EmojiManager

fun Context.emojify(): EmojiManager {
    val app = applicationContext as App
    return app.emojiManager
}