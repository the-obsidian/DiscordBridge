package gg.obsidian.discordbridge

import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

class Plugin : JavaPlugin(), Listener {

    val configuration = Configuration(this)
    var connection: DiscordConnection? = null

    override fun onEnable() {
        updateConfig(description.version)

        this.connection = DiscordConnection(this)

        server.scheduler.runTaskAsynchronously(this, connection)
        server.pluginManager.registerEvents(this, this)
    }

    // Event Handlers

    @EventHandler(priority = EventPriority.MONITOR)
    fun onChat(event: AsyncPlayerChatEvent) {
        logDebug("Received a chat event from ${event.player.name}: ${event.message}")
        val username = ChatColor.stripColor(event.player.name)
        val formattedMessage = configuration.TEMPLATES_DISCORD_CHAT_MESSAGE
                .replace("%u", username)
                .replace("%m", event.message)
        sendToDiscord(formattedMessage)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val username = ChatColor.stripColor(event.player.name)
        logDebug("Received a join event for $username")
        val formattedMessage = configuration.TEMPLATES_DISCORD_PLAYER_JOIN
                .replace("%u", username)
        sendToDiscord(formattedMessage)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val username = ChatColor.stripColor(event.player.name)
        logDebug("Received a leave event for $username")
        val formattedMessage = configuration.TEMPLATES_DISCORD_PLAYER_LEAVE
                .replace("%u", username)
        sendToDiscord(formattedMessage)
    }

    // Message senders

    fun sendToDiscord(message: String) {
        logDebug("Sending message to Discord - $message")
        connection!!.send(message)
    }

    fun sendToMinecraft(username: String, message: String) {
        val formattedMessage = ChatColor.translateAlternateColorCodes('&',
                configuration.TEMPLATES_MINECRAFT_CHAT_MESSAGE
                        .replace("%u", username)
                        .replace("%m", message))
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
