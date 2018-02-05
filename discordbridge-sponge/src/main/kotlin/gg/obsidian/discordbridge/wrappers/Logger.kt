package gg.obsidian.discordbridge.wrappers

import org.slf4j.Logger

class Logger(private val logger: Logger) : ILogger {
    override fun info(message: String) {
        logger.info(message)
    }

    override fun warning(message: String) {
        logger.warn(message)
    }

    override fun warning(message: String, throwable: Throwable) {
        logger.warn(message, throwable)
    }

    override fun severe(message: String) {
        logger.error(message)
    }

    override fun severe(message: String, throwable: Throwable) {
        logger.error(message, throwable)
    }

}