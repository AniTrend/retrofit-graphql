package co.anitrend.retrofit.graphql.sample.initializer

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import co.anitrend.retrofit.graphql.sample.BuildConfig
import fr.bipi.tressence.file.FileLoggerTree
import timber.log.Timber
import java.io.File
import java.io.IOException

class TimberInitializer : Initializer<Unit> {

    @Throws(SecurityException::class)
    private fun getLogsCache(context: Context): File {
        val cache = context.externalCacheDir ?: context.cacheDir
        val logsName = "logs"
        val logs = File(cache, logsName)
        if (!logs.exists()) logs.mkdirs()
        return logs
    }

    @Throws(IOException::class)
    private fun createFileLoggingTree(context: Context) =
        FileLoggerTree.Builder()
            .withFileName("${context.packageName}.log")
            .withDirName(getLogsCache(context).absolutePath)
            .withSizeLimit(logSizeLimit)
            .withFileLimit(1)
            .withMinPriority(logLevel)
            .appendToFile(true)
            .build()

    /**
     * Initializes and a component given the application [Context]
     *
     * @param context The application context.
     */
    override fun create(context: Context) {
        val fileLoggerTree = createFileLoggingTree(context)
        Timber.plant(fileLoggerTree)
        if (BuildConfig.DEBUG)
            Timber.plant(Timber.DebugTree())
    }

    /**
     * @return A list of dependencies that this [Initializer] depends on. This is
     * used to determine initialization order of [Initializer]s.
     *
     * For e.g. if a [Initializer] `B` defines another
     * [Initializer] `A` as its dependency, then `A` gets initialized before `B`.
     */
    override fun dependencies() =
        emptyList<Class<out Initializer<*>>>()

    companion object {
        val logLevel = if (BuildConfig.DEBUG) Log.VERBOSE else Log.WARN
        const val logSizeLimit = 750 * 1024
    }
}