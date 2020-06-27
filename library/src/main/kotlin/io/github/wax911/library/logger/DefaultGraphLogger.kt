package io.github.wax911.library.logger

import android.util.Log
import io.github.wax911.library.logger.contract.ILogger.Level
import io.github.wax911.library.logger.core.AbstractLogger

/**
 * Default logger for the library that writes to [Log]
 * with a default log [level] level of [Level.INFO]
 */
class DefaultGraphLogger(
    level: Level = Level.INFO
) : AbstractLogger(level) {

    /**
     * Write a log message to its destination.
     *
     * @param level Log [Level] filter
     * @param tag Identifier log tag
     * @param message Optional log message
     * @param throwable Optional exception
     */
    override fun log(level: Level, tag: String, message: String, throwable: Throwable?) {
        when (level) {
            Level.VERBOSE -> Log.v(tag, message, throwable)
            Level.DEBUG -> Log.d(tag, message, throwable)
            Level.INFO -> Log.i(tag, message, throwable)
            Level.WARNING -> Log.w(tag, message, throwable)
            Level.ERROR -> Log.e(tag, message, throwable)
            Level.NONE -> { /** no logging at none */ }
        }
    }
}