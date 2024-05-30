/**
 * Copyright 2021 AniTrend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.wax911.library.util

import android.util.Log

/**
 * The levels used to print out and filter log messages
 *
 * The order in terms of verbosity, from least to most is
 * ERROR, WARN, INFO, DEBUG, VERBOSE. Verbose should never be compiled
 * into an application except during development. Debug logs are compiled
 * in but stripped at runtime. Error, warning and info logs are always kept.
 */
@Deprecated(
    "Use ILogger.Level instead",
    ReplaceWith(
        "ILogger.Level",
        "io.github.wax911.library.logger.contract.ILogger.Level",
    ),
)
class LogLevel private constructor(private val value: Int) {
    companion object {
        /** Priority constant for the println method; use Logger.wtf */
        val ASSERT = LogLevel(Log.ASSERT)

        /** Priority constant for the println method; use Logger.d */
        val DEBUG = LogLevel(Log.DEBUG)

        /** Priority constant for the println method; use Logger.e */
        val ERROR = LogLevel(Log.ERROR)

        /** Priority constant for the println method; use Logger.i */
        val INFO = LogLevel(Log.INFO)

        /** Priority constant for the println method; use Logger.v */
        val VERBOSE = LogLevel(Log.VERBOSE)

        /** Priority constant for the println method; use Logger.w */
        val WARN = LogLevel(Log.WARN)
    }

    operator fun compareTo(other: LogLevel) = this.value.compareTo(other.value)
}

@Deprecated(
    "Use LibraryLogger instead",
    ReplaceWith(
        "logger",
        "io.github.wax911.library.logger.core.LibraryLogger",
    ),
)
internal object Logger {
    var level: LogLevel = LogLevel.VERBOSE

    /**
     * Send a {@link #DEBUG} log message and log the exception.
     * @param tag Used to identify the source of a log message. It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    fun d(
        tag: String,
        msg: String,
        tr: Throwable? = null,
    ) {
        if (level < LogLevel.DEBUG) {
            return
        } else {
            Log.d(tag, msg, tr)
        }
    }

    /**
     * Send a {@link #ERROR} log message and log the exception.
     * @param tag Used to identify the source of a log message. It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    fun e(
        tag: String,
        msg: String,
        tr: Throwable? = null,
    ) {
        if (level < LogLevel.ERROR) {
            return
        } else {
            Log.e(tag, msg, tr)
        }
    }

    /**
     * Send a {@link #INFO} log message and log the exception.
     * @param tag Used to identify the source of a log message. It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    fun i(
        tag: String,
        msg: String,
        tr: Throwable? = null,
    ) {
        if (level < LogLevel.INFO) {
            return
        } else {
            Log.i(tag, msg, tr)
        }
    }

    /**
     * Send a {@link #VERBOSE} log message and log the exception.
     * @param tag Used to identify the source of a log message. It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    fun v(
        tag: String,
        msg: String,
        tr: Throwable? = null,
    ) {
        if (level < LogLevel.VERBOSE) {
            return
        } else {
            Log.v(tag, msg, tr)
        }
    }

    /**
     * Send a {@link #WARN} log message and log the exception.
     * @param tag Used to identify the source of a log message. It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    fun w(
        tag: String,
        msg: String,
        tr: Throwable? = null,
    ) {
        if (level < LogLevel.WARN) {
            return
        } else {
            Log.w(tag, msg, tr)
        }
    }

    /**
     * What a Terrible Failure: Report an exception that should never happen.
     * Similar to {@link #wtf(String, Throwable)}, with a message as well.
     * @param tag Used to identify the source of a log message.
     * @param msg The message you would like logged.
     * @param tr An exception to log.  May be null.
     */
    fun wtf(
        tag: String,
        msg: String,
        tr: Throwable? = null,
    ) {
        if (level < LogLevel.ASSERT) {
            return
        } else {
            Log.wtf(tag, msg, tr)
        }
    }
}
