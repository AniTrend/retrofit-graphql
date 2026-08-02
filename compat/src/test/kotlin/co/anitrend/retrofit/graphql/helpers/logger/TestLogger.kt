package co.anitrend.retrofit.graphql.helpers.logger

import co.anitrend.retrofit.graphql.logger.contract.ILogger
import co.anitrend.retrofit.graphql.logger.core.AbstractLogger

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