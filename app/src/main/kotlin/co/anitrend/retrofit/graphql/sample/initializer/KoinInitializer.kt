package co.anitrend.retrofit.graphql.sample.initializer

import android.content.Context
import androidx.startup.Initializer
import co.anitrend.retrofit.graphql.core.helpers.logger.KoinLogger
import co.anitrend.retrofit.graphql.sample.BuildConfig
import co.anitrend.retrofit.graphql.sample.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.fragment.koin.fragmentFactory
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class KoinInitializer : Initializer<KoinApplication> {

    /**
     * Initializes and a component given the application [Context]
     *
     * @param context The application context.
     */
    override fun create(context: Context): KoinApplication {
        val logLevel = if (BuildConfig.DEBUG) Level.DEBUG else Level.INFO
        return startKoin {
            fragmentFactory()
            androidContext(context)
            logger(KoinLogger(logLevel))
            modules(appModules)
        }
    }

    /**
     * @return A list of dependencies that this [Initializer] depends on. This is
     * used to determine initialization order of [Initializer]s.
     *
     * For e.g. if a [Initializer] `B` defines another
     * [Initializer] `A` as its dependency, then `A` gets initialized before `B`.
     */
    override fun dependencies(): List<Class<out Initializer<*>>> =
            listOf(TimberInitializer::class.java)
}