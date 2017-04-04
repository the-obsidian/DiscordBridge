package co.orre.discordbridge.discord.controllers

import co.orre.discordbridge.Plugin
import co.orre.discordbridge.discord.interfaces.BotCommand
import co.orre.discordbridge.discord.interfaces.BotController
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Message

class UtilCommandsController(val plugin: Plugin) : BotController {

    override fun getDescription(): String = ":wrench:  **UTIL** - Utility commands"

    // CONFIRM - Confirm an alias
    @BotCommand(usage="", description="Confirm an alias link request", relayTriggerMessage = false)
    private fun confirm(message: Message) {
        if (!message.isFromType(ChannelType.PRIVATE)) return
        plugin.logDebug("user ${message.author.name} wants to confirm an alias")

        val ua = plugin.requests.find { it.discordId == message.author.id }
        if (ua == null) {
            plugin.sendToDiscord("You have not requested an alias, or your request has expired!", message.channel)
            return
        }

        plugin.saveAlias(ua)
        plugin.requests.remove(ua)
        plugin.sendToDiscord("Successfully linked aliases!", message.channel)
    }

    @BotCommand(usage="", description="You just used it", relayTriggerMessage = false)
    private fun help(message: Message, commands: MutableMap<String, BotControllerManager.Command>,
                     instances: Map<Class<out BotController>, BotController>) {
        plugin.logDebug("user ${message.author.name} requested help")
        plugin.sendToDiscord("**DiscordBridge** - Bridge your Minecraft and Discord chats\n**---**", message.channel)
        var out: String
        for (bc: BotController in instances.values) {
            out = bc.getDescription() + "\n```"
            commands.values.sortedBy { (name) -> name }
                    .filter { it.controllerClass == bc.javaClass }
                    .forEach { out += "\n${it.name} ${it.usage}\n  ${it.description}\n" }
            out += "```"
            plugin.sendToDiscord(out, message.channel)
        }
    }

    // SERVERLIST - List all players currently online on the server
    @BotCommand(usage="", description = "List all Minecraft players currently online on the server",
            relayTriggerMessage = false)
    private fun serverList(message: Message) {
        plugin.logDebug("user ${message.author.name} has requested a listing of online players")

        val players = plugin.getOnlinePlayers()
        if (players.isEmpty()) {
            plugin.sendToDiscord("Nobody is currently online.", message.channel)
            return
        }

        val response = players.joinToString("\n", "The following players are currently online:\n```\n", "\n```")
        plugin.sendToDiscord(response, message.channel)
    }
}