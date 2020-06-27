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