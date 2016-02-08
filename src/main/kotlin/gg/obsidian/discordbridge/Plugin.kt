package gg.obsidian.discordbridge

import org.bukkit.plugin.java.JavaPlugin

class Plugin : JavaPlugin() {

    val configuration = Configuration(this)
    var connection: DiscordConnection? = null

    override fun onEnable() {
        updateConfig(description.version)

        this.connection = DiscordConnection(this)

        server.scheduler.runTaskAsynchronously(this, connection)
        server.pluginManager.registerEvents(EventListener(this), this)
        getCommand("discord").executor = CommandHandler(this)
    }

    fun reload() {
        reloadConfig()
        configuration.load()
        connection?.reconnect()
    }

    // Message senders

    fun sendToDiscord(message: String) {
        logDebug("Sending message to Discord - $message")
        connection!!.send(message)
    }

    fun sendToMinecraft(username: String, message: String) {
        val formattedMessage = Util.formatMessage(
                configuration.TEMPLATES_MINECRAFT_CHAT_MESSAGE,
                mapOf(
                        "%u" to username,
                        "%m" to message
                ),
                colors = true
        )

        server.broadcastMessage(formattedMessage)
    }

    // Utilities

    fun updateConfig(version: String) {
        this.saveDefaultConfig()
        config.options().copyDefaults(true)
        config.set("version", version)
        saveConfig()
        configuration.load()
    }

    fun logDebug(msg: String) {
        if (!configuration.DEBUG) return;
        logger.info(msg)
    }
}
