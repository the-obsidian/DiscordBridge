package gg.obsidian.discordbridge.minecraft

import gg.obsidian.discordbridge.Config
import gg.obsidian.discordbridge.Plugin
import gg.obsidian.discordbridge.commands.AsyncPlayerChatEventWrapper
import gg.obsidian.discordbridge.commands.controllers.BotControllerManager
import gg.obsidian.discordbridge.commands.controllers.FunCommandsController
import gg.obsidian.discordbridge.commands.controllers.UtilCommandsController
import gg.obsidian.discordbridge.discord.Connection
import gg.obsidian.discordbridge.utils.MarkdownToMinecraftSeralizer
import gg.obsidian.discordbridge.utils.UtilFunctions.stripColor
import gg.obsidian.discordbridge.utils.UtilFunctions.toDiscordChatMessage
import gg.obsidian.discordbridge.utils.UtilFunctions.toDiscordPlayerDeath
import gg.obsidian.discordbridge.utils.UtilFunctions.toDiscordPlayerJoin
import gg.obsidian.discordbridge.utils.UtilFunctions.toDiscordPlayerLeave
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.dynmap.DynmapWebChatEvent
import org.bukkit.ChatColor as CC

class EventListener(val plugin: Plugin): Listener {

    val controllerManager = BotControllerManager(plugin)

    init {
        controllerManager.registerController(FunCommandsController(plugin), chatExclusive = true)
        controllerManager.registerController(UtilCommandsController(plugin), chatExclusive = true)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onChat(event: AsyncPlayerChatEvent) {
        plugin.logDebug("Received a chat event from ${event.player.name}: ${event.message}")
        if (!Config.MESSAGES_CHAT) return
        if (event.isCancelled && !Config.RELAY_CANCELLED_MESSAGES) return
        if (event.player.hasMetadata("vanished") && event.player.getMetadata("vanished")[0].asBoolean() &&
                !Config.IF_VANISHED_CHAT) return

        event.message = event.message.replace(":lenny:", "( \u0361\u00B0 \u035C\u0296 \u0361\u00B0)")
                .replace(":tableflip:", "(\u256F\u00B0\u25A1\u00B0\uFF09\u256F\uFE35 \u253B\u2501\u253B")
                .replace(":unflip:", "\u252C\u2500\u2500\u252C \u30CE( \u309C-\u309C\u30CE)")
                .replace(":shrug:", "\u00AF\\_(\u30C4)_/\u00AF")
                .replace(":donger:", "\u30FD\u0F3C\u0E88\u0644\u035C\u0E88\u0F3D\uFF89")
                .replace(":disapproval:", "\u0CA0_\u0CA0")
                .replace(":kawaii:", "(\uFF89\u25D5\u30EE\u25D5)\uFF89*:\uFF65\uFF9F\u2727")
                .replace(":amendo:", "\u0F3C \u3064 \u25D5_\u25D5 \u0F3D\u3064")
                .replace(":yuno:", "\u10DA(\u0CA0\u76CA\u0CA0\u10DA)")
                .replace(":fingerguns:", "(\u261E\uFF9F\u30EE\uFF9F)\u261E")
                .replace(":fingergunsr:", "(\u261E\uFF9F\u30EE\uFF9F)\u261E")
                .replace(":fingergunsl:", "\u261C(\uFF9F\u30EE\uFF9F\u261C)")
                .replace(":fight:", "(\u0E07 \u2022\u0300_\u2022\u0301)\u0E07")
                .replace(":happygary:", "\u1555(\u141B)\u1557")
                .replace(":denko:", "(\u00B4\uFF65\u03C9\uFF65`)")
                .replace(":masteryourdonger:", "(\u0E07 \u0360\u00B0 \u0644\u035C \u00B0)\u0E07")

        val wrapper = AsyncPlayerChatEventWrapper(AsyncPlayerChatEvent(true, event.player, event.message, event.recipients))
        plugin.server.scheduler.runTaskAsynchronously(plugin, { controllerManager.dispatchMessage(wrapper) })

        event.message = MarkdownToMinecraftSeralizer().toMinecraft(plugin.pegDownProc.parseMarkdown(event.message.toCharArray()))
        plugin.logger.info("Seralized! (synchronous)")
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onDynmapCatEvent(event: DynmapWebChatEvent) {
        plugin.sendToDiscord(plugin.translateAliasesToDiscord(event.message.toDiscordChatMessage(event.name, "Dynmap")), Connection.getRelayChannel())
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
