package io.github.wax911.library.helpers.logger

import io.github.wax911.library.logger.contract.ILogger
import io.github.wax911.library.logger.core.AbstractLogger

class TestLogger : AbstractLogger(ILogger.Level.NONE) {
    override fun log(
        level: ILogger.Level,
        tag: String,
        message: String,
        throwable: Throwable?
    ) {
        /** ignored test environment */
    }
}