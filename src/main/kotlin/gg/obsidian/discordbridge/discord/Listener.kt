package gg.obsidian.discordbridge.discord

import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketException
import com.neovisionaries.ws.client.WebSocketFrame
import gg.obsidian.discordbridge.CommandLogic
import gg.obsidian.discordbridge.Plugin
import gg.obsidian.discordbridge.Utils.noSpace
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class Listener(val plugin: Plugin, val api: JDA, val connection: Connection) : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        plugin.logDebug("Received message ${event.message.id} from Discord - ${event.message.rawContent}")

        val rawmsg: String = event.message.rawContent
        val username: String = event.author.name

        // Immediately throw out messages sent from itself or from non-matching servers
        if (username.equals(plugin.cfg.USERNAME, true)) {
            plugin.logDebug("Ignoring message ${event.message.id} from Discord: it matches this bot's username")
            return
        }
//        if (event.guild.id != plugin.cfg.SERVER_ID) {
//            plugin.logDebug("Not relaying message ${event.message.id} from Discord: server does not match")
//            return
//        }

        // NON-RELAY COMMANDS - the following commands and their responses are never sent to Minecraft
        if (rawmsg.startsWith(api.selfUser.asMention, true)) {
            val arg = rawmsg.replaceFirst(api.selfUser.asMention, "").replaceFirst("\\s+", "")

            // CONFIRM - Confirm an alias
            if (arg.startsWith("confirm", true) && event.isFromType(ChannelType.PRIVATE)) {
                plugin.logDebug("user $username wants to confirm an alias")
                val ua = plugin.requests.find { it.discordId == event.author.id }
                if (ua == null) {
                    plugin.sendToDiscord("You have not requested an alias, or your request has expired!", event.privateChannel)
                    return
                }
                plugin.updateAlias(ua)
                plugin.requests.remove(ua)
                plugin.sendToDiscord("Successfully linked aliases!", event.privateChannel)
                return
            }

            // SERVERLIST - List all players currently online on the server
            if (arg.startsWith("serverlist", true)) {
                val channel = if (event.isFromType(ChannelType.PRIVATE)) event.privateChannel else event.channel
                plugin.logDebug("user $username has requested a listing of online players")
                val players = plugin.getOnlinePlayers()
                if (players.isEmpty()) {
                    plugin.sendToDiscord("Nobody is currently online.", channel)
                    return
                }
                val response = players.joinToString("\n", "The following players are currently online:\n```\n", "\n```")
                plugin.sendToDiscord(response, channel)
                return
            }
        }

        // If it is from the relay channel, relay it immediately
        if (event.isFromType(ChannelType.TEXT)) {

            if (!event.textChannel.name.equals(plugin.cfg.CHANNEL, true))
                plugin.logDebug("Not relaying message ${event.message.id} from Discord: channel does not match")
            else {
                plugin.logDebug("Broadcasting message ${event.message.id} from Discord to Minecraft as user $username")
                var alias = plugin.users.data.getString("discordaliases.${event.author.id}.mcusername")
                if (alias == null) alias = username.noSpace()
                plugin.sendToMinecraft(plugin.toMinecraftChatMessage(event.message.content, alias))
            }
        }

        // RELAY COMMANDS - The outputs of these commands WILL be relayed to Minecraft if sent to the relay channel
        if (rawmsg.startsWith(api.selfUser.asMention, true)) {
            val isPrivate = event.isFromType(ChannelType.PRIVATE)
            val channel = if (isPrivate) event.privateChannel else event.channel
            val arg = rawmsg.replaceFirst(api.selfUser.asMention, "").removePrefix(" ")
            if (arg.isEmpty()) return
            plugin.logDebug("Relay command received.  Arg: $arg")

            // SCRIPTED RESPONSE - The bot replies with a preprogrammed response if it detects a corresponding trigger string
            val responses = plugin.scripted_responses.data.getConfigurationSection("responses").getKeys(false)
            var scripted_response: String? = null
            for (r in responses) {
                val casesensitive = plugin.scripted_responses.data.getBoolean("responses.$r.casesensitive", false)
                if (arg.startsWith(plugin.scripted_responses.data.getString("responses.$r.trigger").toLowerCase(), !casesensitive))
                    scripted_response = plugin.scripted_responses.data.getString("responses.$r.response")
            }
            if (scripted_response != null) {
                plugin.logDebug("user $username has triggered the scripted response: $scripted_response")
                plugin.sendToDiscord(scripted_response, channel)
                if (event.isFromType(ChannelType.TEXT) && event.textChannel.name.equals(plugin.cfg.CHANNEL, true))
                    plugin.sendToMinecraft(plugin.toMinecraftChatMessage(scripted_response, plugin.cfg.BOT_MC_USERNAME))
                return
            }

            // CLEVERBOT - Assume anything else invokes Cleverbot
            plugin.logDebug("user $username asks CleverBot something")
            val response = CommandLogic.askCleverbot(plugin.cfg.CLEVERBOT_KEY, arg)
            plugin.sendToDiscord(response, channel)
            // if this occurs in the relay channel, relay the response
            if (event.isFromType(ChannelType.TEXT) && event.textChannel.name.equals(plugin.cfg.CHANNEL, true))
                plugin.sendToMinecraft(plugin.toMinecraftChatMessage(response, plugin.cfg.BOT_MC_USERNAME))
            return
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    fun onUnexpectedError(ws: WebSocket, wse: WebSocketException) {
        plugin.logger.severe("Unexpected error from DiscordBridge: ${wse.message}")
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    fun onDisconnected(webSocket: WebSocket, serverCloseFrame: WebSocketFrame, clientCloseFrame: WebSocketFrame, closedByServer: Boolean) {
        plugin.logDebug("Discord disconnected - attempting to reconnect")
        connection.reconnect()
    }
}
