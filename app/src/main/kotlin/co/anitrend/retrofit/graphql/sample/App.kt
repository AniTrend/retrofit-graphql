package co.anitrend.retrofit.graphql.sample

import androidx.startup.AppInitializer
import co.anitrend.retrofit.graphql.core.SampleApp
import co.anitrend.retrofit.graphql.core.helpers.runtime.UncaughtExceptionHandler
import io.wax911.emojify.EmojiManager
import io.wax911.emojify.initializer.EmojiInitializer

class App : SampleApp() {

    /**
     * Emoji manager instance
     */
    override val emojiManager: EmojiManager by lazy {
        AppInitializer.getInstance(this)
            .initializeComponent(EmojiInitializer::class.java)
    }

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
        if (BuildConfig.DEBUG)
            createUncaughtExceptionHandler()
    }

    /**
     * Uncaught exception handler
     */
    override fun createUncaughtExceptionHandler() {
        val defaultHandler =
            Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(
            UncaughtExceptionHandler(defaultHandler)
        )
    }
}