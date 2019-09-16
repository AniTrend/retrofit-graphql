package io.github.wax911.retgraph

import android.app.Application
import io.github.wax911.retgraph.koin.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class App : Application() {

    /** [Koin](https://insert-koin.io/docs/2.0/getting-started/)
     * Initializes Koin dependency injection
     */
    private fun initKoin() {
        startKoin {
            androidLogger()
            androidContext(applicationContext)
            modules(appModules)
        }
    }

    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}