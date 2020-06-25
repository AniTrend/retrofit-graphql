package co.anitrend.retrofit.graphql.core.helpers.runtime

import timber.log.Timber

/**
 * An uncaught exception handler for the application
 */
internal class UncaughtExceptionHandler(
    private val exceptionHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    /**
     * Method invoked when the given thread terminates due to the given uncaught exception.
     *
     * Any exception thrown by this method will be ignored by the Java Virtual Machine.
     *
     * @param thread the thread
     * @param throwable the exception
     */
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Timber.tag(thread.name).e(throwable)
        exceptionHandler?.uncaughtException(thread, throwable)
    }

    companion object {
        private val TAG =
            UncaughtExceptionHandler::class.java.simpleName
    }
}