package co.orre.discordbridge.minecraft

import co.orre.discordbridge.Config
import co.orre.discordbridge.Plugin
import co.orre.discordbridge.commands.AsyncPlayerChatEventWrapper
import co.orre.discordbridge.commands.controllers.BotControllerManager
import co.orre.discordbridge.commands.controllers.FunCommandsController
import co.orre.discordbridge.discord.Connection
import co.orre.discordbridge.utils.UtilFunctions.stripColor
import co.orre.discordbridge.utils.UtilFunctions.toDiscordPlayerDeath
import co.orre.discordbridge.utils.UtilFunctions.toDiscordPlayerJoin
import co.orre.discordbridge.utils.UtilFunctions.toDiscordPlayerLeave
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.ChatColor as CC

class EventListener(val plugin: Plugin): Listener {

    val controllerManager = BotControllerManager(plugin)

    init { controllerManager.registerController(FunCommandsController(plugin), chatExclusive = true) }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onChat(event: AsyncPlayerChatEvent) {
        plugin.logDebug("Received a chat event from ${event.player.name}: ${event.message}")
        if (!Config.MESSAGES_CHAT) return
        if (event.isCancelled && !Config.RELAY_CANCELLED_MESSAGES) return
        if (event.player.hasMetadata("vanished") && event.player.getMetadata("vanished")[0].asBoolean() &&
                !Config.IF_VANISHED_CHAT) return

        plugin.server.scheduler.runTaskAsynchronously(plugin, { controllerManager.dispatchMessage(AsyncPlayerChatEventWrapper(event)) })
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val username = event.player.name.stripColor()

        plugin.logDebug("Received a join event for $username")
        if (!Config.MESSAGES_JOIN) return
        if (player.hasMetadata("vanished") && player.getMetadata("vanished")[0].asBoolean() &&
                !Config.IF_VANISHED_JOIN) return

        var formattedMessage = player.toDiscordPlayerJoin()
        formattedMessage = plugin.translateAliasesToDiscord(formattedMessage)
        plugin.sendToDiscord(formattedMessage, Connection.getRelayChannel())
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val username = event.player.name.stripColor()

        plugin.logDebug("Received a leave event for $username")
        if (!Config.MESSAGES_LEAVE) return
        if (player.hasMetadata("vanished") && player.getMetadata("vanished")[0].asBoolean() &&
                !Config.IF_VANISHED_LEAVE) return

        var formattedMessage = player.toDiscordPlayerLeave()
        formattedMessage = plugin.translateAliasesToDiscord(formattedMessage)
        plugin.sendToDiscord(formattedMessage, Connection.getRelayChannel())
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val username = event.entity.name.stripColor()
        val deathMessage = event.deathMessage

        plugin.logDebug("Received a death event for $username")
        if (!Config.MESSAGES_DEATH) return
        if (player.hasMetadata("vanished") && player.getMetadata("vanished")[0].asBoolean() &&
                !Config.IF_VANISHED_DEATH) return

        var formattedMessage = deathMessage.toDiscordPlayerDeath(username)
        formattedMessage = plugin.translateAliasesToDiscord(formattedMessage)
        plugin.sendToDiscord(formattedMessage, Connection.getRelayChannel())
    }
}
