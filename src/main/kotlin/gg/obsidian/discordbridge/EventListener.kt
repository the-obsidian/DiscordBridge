package gg.obsidian.discordbridge

import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class EventListener(val plugin: Plugin): Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onChat(event: AsyncPlayerChatEvent) {
        plugin.logDebug("Received a chat event from ${event.player.name}: ${event.message}")


        if (!plugin.configuration.MESSAGES_CHAT) return
        if (event.isCancelled && !plugin.configuration.RELAY_CANCELLED_MESSAGES) return

        // Check for vanished
        val player = event.player;
        if (player.hasMetadata("vanished") &&
                player.getMetadata("vanished")[0].asBoolean() &&
                !plugin.configuration.IF_VANISHED_CHAT) return

        val username = ChatColor.stripColor(event.player.name)
        val formattedMessage = Util.formatMessage(
                plugin.configuration.TEMPLATES_DISCORD_CHAT_MESSAGE,
                mapOf(
                        "%u" to username,
                        "%m" to ChatColor.stripColor(event.message),
                        "%d" to ChatColor.stripColor(player.displayName),
                        "%w" to player.world.name
                )
        )

        plugin.sendToDiscord(formattedMessage)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!plugin.configuration.MESSAGES_JOIN) return

        // Check for vanished
        val player = event.player;
        if (player.hasMetadata("vanished") &&
                player.getMetadata("vanished")[0].asBoolean() &&
                !plugin.configuration.IF_VANISHED_JOIN) return

        val username = ChatColor.stripColor(player.name)
        plugin.logDebug("Received a join event for $username")

        val formattedMessage = Util.formatMessage(
                plugin.configuration.TEMPLATES_DISCORD_PLAYER_JOIN,
                mapOf(
                        "%u" to username,
                        "%d" to ChatColor.stripColor(player.displayName)
                )
        )

        plugin.sendToDiscord(formattedMessage)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (!plugin.configuration.MESSAGES_LEAVE) return

        // Check for vanished
        val player = event.player;
        if (player.hasMetadata("vanished") &&
                player.getMetadata("vanished")[0].asBoolean() &&
                !plugin.configuration.IF_VANISHED_LEAVE) return

        val username = ChatColor.stripColor(event.player.name)
        plugin.logDebug("Received a leave event for $username")

        val formattedMessage = Util.formatMessage(
                plugin.configuration.TEMPLATES_DISCORD_PLAYER_LEAVE,
                mapOf(
                        "%u" to username,
                        "%d" to ChatColor.stripColor(event.player.displayName)
                )
        )

        plugin.sendToDiscord(formattedMessage)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (!plugin.configuration.MESSAGES_DEATH) return

        // Check for vanished
        val player = event.entity;
        if (player.hasMetadata("vanished") &&
                player.getMetadata("vanished")[0].asBoolean() &&
                !plugin.configuration.IF_VANISHED_DEATH) return

        val username = ChatColor.stripColor(event.entity.name)
        plugin.logDebug("Received a death event for $username")

        val formattedMessage = Util.formatMessage(
                plugin.configuration.TEMPLATES_DISCORD_PLAYER_DEATH,
                mapOf(
                        "%u" to username,
                        "%d" to ChatColor.stripColor(event.entity.displayName),
                        "%r" to event.deathMessage,
                        "%w" to event.entity.world.name
                )
        )

        plugin.sendToDiscord(formattedMessage)
    }
}
