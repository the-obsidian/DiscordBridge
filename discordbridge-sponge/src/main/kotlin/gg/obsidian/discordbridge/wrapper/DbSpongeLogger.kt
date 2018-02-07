package gg.obsidian.discordbridge.wrapper

import org.slf4j.Logger

class DbSpongeLogger(private val logger: Logger) : IDbLogger {
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
