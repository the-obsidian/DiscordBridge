package co.orre.discordbridge.commands.controllers

import co.orre.discordbridge.Plugin
import co.orre.discordbridge.UserAliasConfig
import co.orre.discordbridge.commands.AsyncPlayerChatEventWrapper
import co.orre.discordbridge.commands.IBotController
import co.orre.discordbridge.commands.IEventWrapper
import co.orre.discordbridge.commands.MessageWrapper
import co.orre.discordbridge.commands.annotations.BotCommand
import co.orre.discordbridge.commands.annotations.ChatExclusiveCommand
import co.orre.discordbridge.commands.annotations.DiscordExclusiveCommand
import co.orre.discordbridge.commands.annotations.PrivateResponse
import net.dv8tion.jda.core.entities.ChannelType
import org.bukkit.ChatColor as CC

class UtilCommandsController(val plugin: Plugin) : IBotController {

    override fun getDescription(): String = ":wrench:  **UTIL** - Utility commands"

    // CONFIRM - Confirm an alias request
    @BotCommand(usage="", description="Confirm an alias link request", relayTriggerMessage = false)
    @DiscordExclusiveCommand
    @PrivateResponse
    private fun confirm(message: MessageWrapper): String? {
        if (!message.originalMessage.isFromType(ChannelType.PRIVATE)) return null
        plugin.logDebug("user ${message.senderName} confirms an alias request")

        val ua = plugin.requests.find { it.discordId == message.senderId } ?: return "You have no alias requests pending."

        UserAliasConfig.add(plugin, ua)
        plugin.requests.remove(ua)
        return "Successfully linked aliases!"
    }

    // DENY - Deny an alias request
    @BotCommand(usage="", description="Deny an alias link request", relayTriggerMessage = false)
    @DiscordExclusiveCommand
    @PrivateResponse
    private fun deny(message: MessageWrapper): String? {
        if (!message.originalMessage.isFromType(ChannelType.PRIVATE)) return null
        plugin.logDebug("user ${message.senderName} denies an alias request")

        val ua = plugin.requests.find { it.discordId == message.senderId } ?: return "You have no alias requests pending."

        plugin.requests.remove(ua)
        return "The alias link request has been cancelled."
    }

    @BotCommand(usage="", description="You just used it", relayTriggerMessage = false)
    @ChatExclusiveCommand
    @PrivateResponse
    private fun help(event: IEventWrapper, commands: MutableMap<String, BotControllerManager.Command>,
                     instances: Map<Class<out IBotController>, IBotController>) {
        plugin.logDebug("user ${event.senderName} requested help")
        when (event) {
            is AsyncPlayerChatEventWrapper -> {
                val player = event.event.player
                player.sendMessage("${CC.BOLD}${CC.AQUA}DiscordBridge${CC.RESET} - Bridge your Minecraft and Discord chats\n---")
                var out: String
                for (bc: IBotController in instances.values) {
                    out = bc.getDescription() + "\n```"
                    commands.values.sortedBy { (name) -> name }
                            .filter { it.controllerClass == bc.javaClass }
                            .forEach { out += "\n${it.name} ${it.usage}\n  ${it.description}\n" }
                    out += "```"
                    player.sendMessage(out)
                }
            }
            is MessageWrapper -> {
                event.originalMessage.author.openPrivateChannel()
                val channel = event.originalMessage.author.privateChannel
                channel.sendMessage("**DiscordBridge** - Bridge your Minecraft and Discord chats\n**---**")
                var out: String
                for (bc: IBotController in instances.values) {
                    out = bc.getDescription() + "\n```"
                    commands.values.sortedBy { (name) -> name }
                            .filter { it.controllerClass == bc.javaClass }
                            .forEach { out += "\n${it.name} ${it.usage}\n  ${it.description}\n" }
                    out += "```"
                    channel.sendMessage(out)
                }
            }
        }
    }

    // SERVERLIST - List all players currently online on the server
    @DiscordExclusiveCommand
    @BotCommand(usage="", description = "List all Minecraft players currently online on the server",
            relayTriggerMessage = false)
    private fun serverList(message: MessageWrapper) {
        plugin.logDebug("user ${message.originalMessage.author.name} has requested a listing of online players")

        val players = plugin.getOnlinePlayers()
        if (players.isEmpty()) {
            plugin.sendToDiscord("Nobody is currently online.", message.channel)
            return
        }

        val response = players.joinToString("\n", "The following players are currently online:\n```\n", "\n```")
        plugin.sendToDiscord(response, message.channel)
    }
}