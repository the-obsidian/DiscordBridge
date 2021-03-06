package gg.obsidian.discordbridge.wrapper

import java.util.logging.Level
import java.util.logging.Logger

class DbBukkitLogger(private val logger: Logger) : IDbLogger {
    override fun info(message: String) {
        logger.info(message)
    }

    override fun warning(message: String) {
        logger.warning(message)
    }

    override fun warning(message: String, throwable: Throwable) {
        logger.log(Level.WARNING, message, throwable)
    }

    override fun severe(message: String) {
        logger.severe(message)
    }

    override fun severe(message: String, throwable: Throwable) {
        logger.log(Level.SEVERE, message, throwable)
    }
}
