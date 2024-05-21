package co.anitrend.retrofit.graphql.sample.initializer

import android.content.Context
import androidx.startup.Initializer
import co.anitrend.retrofit.graphql.sample.BuildConfig
import timber.log.Timber

class TimberInitializer : Initializer<Unit> {

    /**
     * Initializes and a component given the application [Context]
     *
     * @param context The application context.
     */
    override fun create(context: Context) {
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
}