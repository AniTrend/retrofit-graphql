package co.anitrend.retrofit.graphql.core.helpers.logger

import org.koin.core.logger.Level
import org.koin.core.logger.Logger
import org.koin.core.logger.MESSAGE
import timber.log.Timber

/**
 * A logger proxy for Timber
 */
internal class KoinLogger(
    logLevel: Level
) : Logger(logLevel) {
    override fun display(level: Level, msg: MESSAGE) {
        when (level) {
            Level.DEBUG -> Timber.d(msg)
            Level.INFO -> Timber.i(msg)
            Level.ERROR -> Timber.e(msg)
            Level.NONE -> {}
            Level.WARNING -> Timber.w(msg)
        }
    }
}