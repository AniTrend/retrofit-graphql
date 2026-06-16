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

package io.github.wax911.library.logger

import android.util.Log
import io.github.wax911.library.logger.contract.ILogger.Level
import io.github.wax911.library.logger.core.AbstractLogger

/**
 * Default logger for the library that writes to [Log]
 * with a default log [level] level of [Level.INFO]
 */
class DefaultGraphLogger(
    level: Level = Level.INFO,
) : AbstractLogger(level) {
    /**
     * Write a log message to its destination.
     *
     * @param level Log [Level] filter
     * @param tag Identifier log tag
     * @param message Optional log message
     * @param throwable Optional exception
     */
    override fun log(
        level: Level,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        when (level) {
            Level.VERBOSE -> Log.v(tag, message, throwable)
            Level.DEBUG -> Log.d(tag, message, throwable)
            Level.INFO -> Log.i(tag, message, throwable)
            Level.WARNING -> Log.w(tag, message, throwable)
            Level.ERROR -> Log.e(tag, message, throwable)
            Level.NONE -> { /* no logging at none */ }
        }
    }
}
