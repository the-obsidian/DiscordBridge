package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.Utils.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.lang.reflect.Method

class EventListener(val plugin: Plugin) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onChat(event: AsyncPlayerChatEvent) {
        plugin.logDebug("Received a chat event from ${event.player.name}: ${event.message}")


        if (!plugin.cfg.MESSAGES_CHAT) return
        if (event.isCancelled && !plugin.cfg.RELAY_CANCELLED_MESSAGES) return

        // Check for vanished
        val player = event.player
        if (player.hasMetadata("vanished") &&
                player.getMetadata("vanished")[0].asBoolean() &&
                !plugin.cfg.IF_VANISHED_CHAT) return

        val username = event.player.name.stripColor()
        var worldname = player.world.name
        if (plugin.isMultiverse()) {
            val worldProperties = plugin.worlds!!.data.get("worlds.$worldname")
            val cls = Class.forName("com.onarandombox.MultiverseCore.WorldProperties")
            val meth: Method = cls.getMethod("getAlias")
            val alias = meth.invoke(worldProperties)
            if (alias is String) worldname = alias
        }

        val formattedMessage = Utils.formatMessage(
                plugin.cfg.TEMPLATES_DISCORD_CHAT_MESSAGE,
                mapOf(
                        "%u" to username,
                        "%m" to event.message.stripColor(),
                        "%d" to player.displayName.stripColor(),
                        "%w" to worldname
                )
        )

        plugin.sendToDiscord(formattedMessage, event.player.uniqueId.toString())
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!plugin.cfg.MESSAGES_JOIN) return

        // Check for vanished
        val player = event.player
        if (player.hasMetadata("vanished") &&
                player.getMetadata("vanished")[0].asBoolean() &&
                !plugin.cfg.IF_VANISHED_JOIN) return

        val username = player.name.stripColor()
        plugin.logDebug("Received a join event for $username")

        val formattedMessage = Utils.formatMessage(
                plugin.cfg.TEMPLATES_DISCORD_PLAYER_JOIN,
                mapOf(
                        "%u" to username,
                        "%d" to player.displayName.stripColor()
                )
        )

        plugin.sendToDiscord(formattedMessage, player.uniqueId.toString())
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (!plugin.cfg.MESSAGES_LEAVE) return

        // Check for vanished
        val player = event.player
        if (player.hasMetadata("vanished") &&
                player.getMetadata("vanished")[0].asBoolean() &&
                !plugin.cfg.IF_VANISHED_LEAVE) return

        val username = event.player.name.stripColor()
        plugin.logDebug("Received a leave event for $username")

        val formattedMessage = Utils.formatMessage(
                plugin.cfg.TEMPLATES_DISCORD_PLAYER_LEAVE,
                mapOf(
                        "%u" to username,
                        "%d" to event.player.displayName.stripColor()
                )
        )

        plugin.sendToDiscord(formattedMessage, player.uniqueId.toString())
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (!plugin.cfg.MESSAGES_DEATH) return

        // Check for vanished
        val player = event.entity
        if (player.hasMetadata("vanished") &&
                player.getMetadata("vanished")[0].asBoolean() &&
                !plugin.cfg.IF_VANISHED_DEATH) return

        val username = event.entity.name.stripColor()
        plugin.logDebug("Received a death event for $username")

        val formattedMessage = Utils.formatMessage(
                plugin.cfg.TEMPLATES_DISCORD_PLAYER_DEATH,
                mapOf(
                        "%u" to username,
                        "%d" to event.entity.displayName.stripColor(),
                        "%r" to event.deathMessage,
                        "%w" to event.entity.world.name
                )
        )

        plugin.sendToDiscord(formattedMessage, player.uniqueId.toString())
    }
}
