package co.orre.discordbridge.minecraft

import co.orre.discordbridge.utils.UtilFunctions.stripColor
import co.orre.discordbridge.utils.UtilFunctions.noSpace
import co.orre.discordbridge.CommandLogic
import co.orre.discordbridge.Config
import co.orre.discordbridge.Plugin
import co.orre.discordbridge.discord.Connection
import co.orre.discordbridge.minecraft.controllers.BotControllerManager
import co.orre.discordbridge.minecraft.controllers.FunCommandsController
import co.orre.discordbridge.utils.UtilFunctions.toDiscordChatMessage
import co.orre.discordbridge.utils.UtilFunctions.toDiscordPlayerDeath
import co.orre.discordbridge.utils.UtilFunctions.toDiscordPlayerJoin
import co.orre.discordbridge.utils.UtilFunctions.toDiscordPlayerLeave
import co.orre.discordbridge.utils.UtilFunctions.toMinecraftChatMessage
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.ChatColor as CC
import java.lang.reflect.Method

class EventListener(val plugin: Plugin): Listener {

    val controllerManager = BotControllerManager(plugin)

    init {
        controllerManager.registerController(FunCommandsController(plugin))
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onChat(event: AsyncPlayerChatEvent) {

        plugin.logDebug("Received a chat event from ${event.player.name}: ${event.message}")
        if (!Config.MESSAGES_CHAT) return
        if (event.isCancelled && !Config.RELAY_CANCELLED_MESSAGES) return
        if (event.player.hasMetadata("vanished") && event.player.getMetadata("vanished")[0].asBoolean() &&
                !Config.IF_VANISHED_CHAT) return

        // Get world alias if Multiverse is installed
//        if (plugin.isMultiverseInstalled) {
//            val worldProperties = plugin.worlds!!.data.get("worlds.$worldname")
//            val cls = Class.forName("com.onarandombox.MultiverseCore.WorldProperties")
//            val meth: Method = cls.getMethod("getAlias")
//            val alias = meth.invoke(worldProperties)
//            if (alias is String) worldname = alias
//        }

        controllerManager.dispatchMessage(event)

        var formattedMessage = message.toDiscordChatMessage(username, displayName, worldname)
        formattedMessage = plugin.convertAtMentions(formattedMessage)
        formattedMessage = plugin.translateAliasToDiscord(formattedMessage, uuid)
        plugin.sendToDiscord(formattedMessage, Connection.getRelayChannel())

        // If it was a @mention to the bot, treat it as a Cleverbot invocation
        val task = Runnable {
            if (Permissions.cleverbot.has(player)) {
                val arg: String = message.removePrefix("@" + Config.USERNAME.noSpace()).trimStart()
                val response = CommandLogic.askCleverbot(arg)
                plugin.sendToMinecraft(response.toMinecraftChatMessage(Config.BOT_MC_USERNAME))
                plugin.sendToDiscord(response, Connection.getRelayChannel())
            } else
                player.sendMessage("${org.bukkit.ChatColor.RED}You do not have permission to talk to the bot.")
        }
        plugin.server.scheduler.runTaskAsynchronously(plugin, task)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val username = event.player.name.stripColor()
        val uuid = event.player.uniqueId.toString()

        plugin.logDebug("Received a join event for $username")
        if (!Config.MESSAGES_JOIN) return
        if (player.hasMetadata("vanished") && player.getMetadata("vanished")[0].asBoolean() &&
                !Config.IF_VANISHED_JOIN) return

        var formattedMessage = player.toDiscordPlayerJoin()
        formattedMessage = plugin.translateAliasToDiscord(formattedMessage, uuid)
        plugin.sendToDiscord(formattedMessage, Connection.getRelayChannel())
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val username = event.player.name.stripColor()
        val uuid = event.player.uniqueId.toString()

        plugin.logDebug("Received a leave event for $username")
        if (!Config.MESSAGES_LEAVE) return
        if (player.hasMetadata("vanished") && player.getMetadata("vanished")[0].asBoolean() &&
                !Config.IF_VANISHED_LEAVE) return

        var formattedMessage = player.toDiscordPlayerLeave()
        formattedMessage = plugin.translateAliasToDiscord(formattedMessage, uuid)
        plugin.sendToDiscord(formattedMessage, Connection.getRelayChannel())
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val username = event.entity.name.stripColor()
        val displayName = event.entity.displayName.stripColor()
        val uuid = event.entity.uniqueId.toString()
        val deathMessage = event.deathMessage

        plugin.logDebug("Received a death event for $username")
        if (!Config.MESSAGES_DEATH) return
        if (player.hasMetadata("vanished") && player.getMetadata("vanished")[0].asBoolean() &&
                !Config.IF_VANISHED_DEATH) return

        var formattedMessage = deathMessage.toDiscordPlayerDeath(username, displayName)
        formattedMessage = plugin.translateAliasToDiscord(formattedMessage, uuid)
        plugin.sendToDiscord(formattedMessage, Connection.getRelayChannel())
    }
}
