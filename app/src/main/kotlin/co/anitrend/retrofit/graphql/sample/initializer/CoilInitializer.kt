package co.anitrend.retrofit.graphql.sample.initializer

import android.content.Context
import androidx.startup.Initializer
import coil.Coil
import coil.ImageLoaderFactory
import org.koin.core.KoinComponent
import org.koin.core.get

internal class CoilInitializer : Initializer<Unit> {

    /**
     * Initializes and a component given the application [Context]
     *
     * @param context The application context.
     */
    override fun create(context: Context) {
        // I could just have koin component declared on the class level
        val component = object : KoinComponent {}
        val factory =
            component.get<ImageLoaderFactory>()
        Coil.setImageLoader(factory)
    }

    /**
     * @return A list of dependencies that this [Initializer] depends on. This is
     * used to determine initialization order of [Initializer]s.
     *
     * For e.g. if a [Initializer] `B` defines another
     * [Initializer] `A` as its dependency, then `A` gets initialized before `B`.
     */
    override fun dependencies(): List<Class<out Initializer<*>>> =
            listOf(KoinInitializer::class.java)
}