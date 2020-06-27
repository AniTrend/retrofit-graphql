package io.github.wax911.library.logger.core

import io.github.wax911.library.logger.contract.ILogger
import org.junit.Assert.assertEquals
import org.junit.Test

class AbstractLoggerTest {

    private val logger = object : AbstractLogger(ILogger.Level.INFO) {
        override fun log(
            level: ILogger.Level,
            tag: String,
            message: String,
            throwable: Throwable?
        ) { /** ignored */ }
    }

    @Test
    fun `assert given a log level equal to the current level returns true`() {
        val given = ILogger.Level.INFO
        val expected = true
        val actual = logger.isLoggable(given)
        assertEquals(expected, actual)
    }

    @Test
    fun `assert given a log level less than to the logger level returns false`() {
        val given = ILogger.Level.DEBUG
        val expected = false
        val actual = logger.isLoggable(given)
        assertEquals(expected, actual)
    }

    @Test
    fun `assert given a log level greater than to the logger level returns true`() {
        val given = ILogger.Level.ERROR
        val expected = true
        val actual = logger.isLoggable(given)
        assertEquals(expected, actual)
    }
}