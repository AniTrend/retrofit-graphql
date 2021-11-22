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

package io.github.wax911.library.logger.contract

/**
 * Logger contract
 */
interface ILogger {
    var level: Level

    /**
     * Write a log message to its destination.
     *
     * @param level Filter used to determine the verbosity level of logs.
     * @param tag Used to identify the source of a log message. It usually
     * identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     * @param throwable An exception to log
     */
    fun log(level: Level, tag: String, message: String, throwable: Throwable? = null)

    /**
     * The levels used to print out and filter log messages
     */
    enum class Level {
        VERBOSE, DEBUG, INFO, WARNING, ERROR, NONE
    }
}
