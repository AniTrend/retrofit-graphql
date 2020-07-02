package io.github.wax911.library.logger.core

import androidx.annotation.VisibleForTesting
import io.github.wax911.library.logger.contract.ILogger
import io.github.wax911.library.logger.contract.ILogger.Level

/**
 * Abstract logger for the library
 *
 * @param level minimum level to write to destination
 */
abstract class AbstractLogger(
    override var level: Level
) : ILogger {

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun isLoggable(level: Level): Boolean = this.level <= level

    private fun printLog(
        level: Level,
        tag: String,
        message: String,
        throwable: Throwable? = null
    ) {
        if (isLoggable(level))
            log(level, tag, message, throwable)
    }

    /**
     * Send a [Level.VERBOSE] log message and log the exception.
     *
     * @param tag Used to identify the source of a log message. It usually
     * identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     * @param throwable An exception to log
     */
    fun v(tag: String, message: String, throwable: Throwable? = null) {
        printLog(Level.VERBOSE, tag, message, throwable)
    }

    /**
     * Send a [Level.DEBUG] log message and log the exception.
     *
     * @param tag Used to identify the source of a log message. It usually
     * identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     * @param throwable An exception to log
     */
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        printLog(Level.DEBUG, tag, message, throwable)
    }

    /**
     * Send a [Level.INFO] log message and log the exception.
     *
     * @param tag Used to identify the source of a log message. It usually
     * identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     * @param throwable An exception to log
     */
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        printLog(Level.INFO, tag, message, throwable)
    }

    /**
     * Send a [Level.WARNING] log message and log the exception.
     *
     * @param tag Used to identify the source of a log message. It usually
     * identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     * @param throwable An exception to log
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        printLog(Level.WARNING, tag, message, throwable)
    }

    /**
     * Send a [Level.ERROR] log message and log the exception.
     *
     * @param tag Used to identify the source of a log message. It usually
     * identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     * @param throwable An exception to log
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        printLog(Level.ERROR, tag, message, throwable)
    }
}