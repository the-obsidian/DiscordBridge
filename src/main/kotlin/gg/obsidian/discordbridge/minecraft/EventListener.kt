package gg.obsidian.discordbridge.minecraft

import gg.obsidian.discordbridge.CommandLogic
import gg.obsidian.discordbridge.Plugin
import gg.obsidian.discordbridge.Utils.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.ChatColor as CC
import java.lang.reflect.Method

class EventListener(val plugin: Plugin) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        val username = event.player.name.stripColor()
        val displayName = event.player.displayName.stripColor()
        val uuid = event.player.uniqueId.toString()
        val message = event.message.stripColor()
        var worldname = event.player.world.name

        plugin.logDebug("Received a chat event from $username: $message")
        if (!plugin.cfg.MESSAGES_CHAT) return
        if (event.isCancelled && !plugin.cfg.RELAY_CANCELLED_MESSAGES) return
        if (player.hasMetadata("vanished") && player.getMetadata("vanished")[0].asBoolean() &&
                !plugin.cfg.IF_VANISHED_CHAT) return

        // Get world alias if Multiverse is installed
        if (plugin.foundMultiverse) {
            val worldProperties = plugin.worlds!!.data.get("worlds.$worldname")
            val cls = Class.forName("com.onarandombox.MultiverseCore.WorldProperties")
            val meth: Method = cls.getMethod("getAlias")
            val alias = meth.invoke(worldProperties)
            if (alias is String) worldname = alias
        }

        var formattedMessage = plugin.toDiscordChatMessage(message, username, displayName, worldname)
        formattedMessage = plugin.convertAtMentions(formattedMessage)
        formattedMessage = plugin.translateAliasToDiscord(formattedMessage, uuid)
        plugin.sendToDiscord(formattedMessage, plugin.conn.getRelayChannel())

        // If it was a @mention to the bot, treat it as a Cleverbot invocation
        if (message.startsWith("@" + plugin.cfg.USERNAME.noSpace())) {
            val task = Runnable {
                if (Permissions.cleverbot.has(player)) {
                    val arg: String = message.removePrefix("@" + plugin.cfg.USERNAME.noSpace()).trimStart()
                    val response = CommandLogic.askCleverbot(plugin, arg)
                    plugin.sendToMinecraft(plugin.toMinecraftChatMessage(response, plugin.cfg.BOT_MC_USERNAME))
                    plugin.sendToDiscord(response, plugin.conn.getRelayChannel())
                } else
                    player.sendMessage("${CC.RED}You do not have permission to talk to the bot.")
            }
            plugin.server.scheduler.runTaskAsynchronously(plugin, task)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val username = event.player.name.stripColor()
        val displayName = event.player.displayName.stripColor()
        val uuid = event.player.uniqueId.toString()

        plugin.logDebug("Received a join event for $username")
        if (!plugin.cfg.MESSAGES_JOIN) return
        if (player.hasMetadata("vanished") && player.getMetadata("vanished")[0].asBoolean() &&
                !plugin.cfg.IF_VANISHED_JOIN) return

        var formattedMessage = plugin.toDiscordPlayerJoin(username, displayName)
        formattedMessage = plugin.translateAliasToDiscord(formattedMessage, uuid)
        plugin.sendToDiscord(formattedMessage, plugin.conn.getRelayChannel())
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val username = event.player.name.stripColor()
        val displayName = event.player.displayName.stripColor()
        val uuid = event.player.uniqueId.toString()

        plugin.logDebug("Received a leave event for $username")
        if (!plugin.cfg.MESSAGES_LEAVE) return
        if (player.hasMetadata("vanished") && player.getMetadata("vanished")[0].asBoolean() &&
                !plugin.cfg.IF_VANISHED_LEAVE) return

        var formattedMessage = plugin.toDiscordPlayerLeave(username, displayName)
        formattedMessage = plugin.translateAliasToDiscord(formattedMessage, uuid)
        plugin.sendToDiscord(formattedMessage, plugin.conn.getRelayChannel())
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val username = event.entity.name.stripColor()
        val displayName = event.entity.displayName.stripColor()
        val uuid = event.entity.uniqueId.toString()
        val deathMessage = event.deathMessage

        plugin.logDebug("Received a death event for $username")
        if (!plugin.cfg.MESSAGES_DEATH) return
        if (player.hasMetadata("vanished") && player.getMetadata("vanished")[0].asBoolean() &&
                !plugin.cfg.IF_VANISHED_DEATH) return

        var formattedMessage = plugin.toDiscordPlayerDeath(deathMessage, username, displayName)
        formattedMessage = plugin.translateAliasToDiscord(formattedMessage, uuid)
        plugin.sendToDiscord(formattedMessage, plugin.conn.getRelayChannel())
    }
}
