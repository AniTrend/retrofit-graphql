package co.anitrend.retrofit.graphql.core

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import io.wax911.emojify.EmojiManager

abstract class SampleApp : Application() {

    /**
     * Emoji manager instance
     */
    internal abstract val emojiManager: EmojiManager

    /**
     * Uncaught exception handler
     */
    protected abstract fun createUncaughtExceptionHandler()

    /**
     * Called when the application is starting, before any activity, service,
     * or receiver objects (excluding content providers) have been created.
     *
     * Implementations should be as quick as possible (for example using
     * lazy initialization of state) since the time spent in this function
     * directly impacts the performance of starting the first activity,
     * service, or receiver in a process.
     *
     * If you override this method, be sure to call `super.onCreate()`.
     *
     * Be aware that direct boot may also affect callback order on
     * Android [android.os.Build.VERSION_CODES.N] and later devices.
     * Until the user unlocks the device, only direct boot aware components are
     * allowed to run. You should consider that all direct boot unaware
     * components, including such [android.content.ContentProvider], are
     * disabled until user unlock happens, especially when component callback
     * order matters.
     */
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(
            AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}