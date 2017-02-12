package gg.obsidian.discordbridge

import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketException
import com.neovisionaries.ws.client.WebSocketFrame
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class DiscordListener(val plugin: Plugin, val api: JDA, val connection: DiscordConnection) : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        plugin.logDebug("Received message ${event.message.id} from Discord - ${event.message.rawContent}")

        val rawmsg: String = event.message.rawContent
        val username: String = event.author.name

        // Immediately throw out messages sent from itself or from non-matching servers
        if (username.equals(plugin.configuration.USERNAME, true)) {
            plugin.logDebug("Ignoring message ${event.message.id} from Discord: it matches this bot's username")
            return
        }
        if (event.guild.id != plugin.configuration.SERVER_ID) {
            plugin.logDebug("Not relaying message ${event.message.id} from Discord: server does not match")
            return
        }

        // NON-RELAY COMMANDS - the following commands and their responses are not sent to Minecraft
        if(rawmsg.startsWith(api.selfUser.asMention, true)) {
            val arg = rawmsg.replaceFirst(api.selfUser.asMention, "").replaceFirst("\\s+", "")

            // CONFIRM - Confirm an alias
            if (arg.startsWith("confirm", true) && event.isFromType(ChannelType.PRIVATE)) {
                plugin.logDebug("user $username wants to confirm an alias")
                val ua = plugin.requests.find { it.discordId == event.author.id }
                if (ua == null) {
                    plugin.sendToDiscordRespond("You have not requested an alias, or your request has expired!", event)
                    return
                }
                plugin.updateAlias(ua)
                plugin.requests.remove(ua)
                plugin.sendToDiscordRespond("Successfully linked aliases!", event)
                return
            }

            // SERVERLIST - List all players currently online on the server
            if (arg.startsWith("serverlist", true)) {
                plugin.logDebug("user $username has requested a listing of online players")
                val players = plugin.getOnlinePlayers()
                if (players.isEmpty()) {
                    plugin.sendToDiscordRespond("Nobody is currently online.", event)
                    return
                }
                val response = players.joinToString("\n", "The following players are currently online:\n```\n", "\n```")
                plugin.sendToDiscordRespond(response, event)
                return
            }
        }

        // If it is from the relay channel, relay it immediately
        if (event.isFromType(ChannelType.TEXT)) {

            if (!event.textChannel.name.equals(plugin.configuration.CHANNEL, true))
                plugin.logDebug("Not relaying message ${event.message.id} from Discord: channel does not match")
            else {
                plugin.logDebug("Broadcasting message ${event.message.id} from Discord to Minecraft as user $username")
                plugin.sendToMinecraft(username, event.author.id, event.message.content)
            }
        }

        // RELAY COMMANDS - These commands and their outputs DO get relayed to Minecraft
        if(rawmsg.startsWith(api.selfUser.asMention, true)) {
            val arg = rawmsg.replaceFirst(api.selfUser.asMention, "").removePrefix(" ")
            if (arg.isEmpty()) return
            plugin.logDebug("Relay command received.  Arg: $arg")

            // OIKOS - Delicious Greek yogurt from Danone!
            if (arg.startsWith("oikos", true)) {
                plugin.logDebug("user $username has initiated oikos!")
                plugin.sendToDiscordRespond("Delicious Greek yogurt from Danone!", event)
                plugin.sendToMinecraftBroadcast("Delicious Greek yogurt from Danone!")
                return
            }
            if (arg.startsWith("delicious greek yogurt from danone", true)) {
                plugin.logDebug("user $username has initiated oikos 2!")
                plugin.sendToDiscordRespond("\uD83D\uDE04", event)
                plugin.sendToMinecraftBroadcast(":D")
                return
            }
            if (arg == "\uD83D\uDE04") {
                plugin.logDebug("user $username has initiated oikos 3!")
                plugin.sendToDiscordRespond("\uD83D\uDE04", event)
                plugin.sendToMinecraftBroadcast(":D")
                return
            }

            // CLEVERBOT - Assume anything else invokes Cleverbot
            plugin.logDebug("user $username asks CleverBot something")
            val response = Util.askCleverbot(plugin.configuration.CLEVERBOT_KEY, arg)
            plugin.sendToDiscordRespond(response, event)
            // if this occurs in the relay channel, relay the response
            if (event.isFromType(ChannelType.TEXT) && event.textChannel.name.equals(plugin.configuration.CHANNEL, true))
                plugin.sendToMinecraftBroadcast(response)
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
