package gg.obsidian.discordbridge

import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketException
import com.neovisionaries.ws.client.WebSocketFrame
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

@Suppress("unused", "UNUSED_PARAMETER")
class DiscordListener(val plugin: Plugin, val api: JDA, val connection: DiscordConnection) : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        plugin.logDebug("Received message ${event.message.id} from Discord")

        val rawmsg: String = event.message.rawContent
        val msg: String = event.message.content
        val username: String = event.member.effectiveName

        if (rawmsg.startsWith("<@267902537074606082> confirm", true) && event.isFromType(ChannelType.PRIVATE)) {
            plugin.logDebug("user $username wants to confirm an alias")
            val ua = plugin.requests.find {it.discordId == event.author.id}
            if (ua == null) {
                plugin.sendToDiscordRespond("You have not requested an alias, or your request has expired!", event)
                return
            }
            plugin.updateAlias(ua)
            plugin.requests.remove(ua)
            plugin.sendToDiscordRespond("Successfully linked aliases!", event)
            return
        }

        if (rawmsg.startsWith("<@267902537074606082> serverlist", true)) {
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

        if (rawmsg.startsWith("<@267902537074606082> oikos", true)) {
            plugin.logDebug("user $username has initiated oikos!")
            plugin.sendToDiscordRespond("Delicious Greek yogurt from Danone!", event)
            return
        }

        if (rawmsg == "<@267902537074606082> :smile:") {
            plugin.logDebug("user $username has initiated Oikos Part 2!")
            plugin.sendToDiscordRespond(":smile:", event)
            return
        }

        if (event.guild.id != plugin.configuration.SERVER_ID) {
            plugin.logDebug("Ignoring message ${event.message.id} from Discord: server does not match")
            return
        }

        if (!event.textChannel.name.equals(plugin.configuration.CHANNEL, true)) {
            plugin.logDebug("Ignoring message ${event.message.id} from Discord: channel does not match")
            return
        }

        if (username.equals(plugin.configuration.USERNAME, true)) {
            plugin.logDebug("Ignoring message ${event.message.id} from Discord: it matches the server's username")
            return
        }

        plugin.logDebug("Broadcasting message ${event.message.id} from Discord as user $username")
        plugin.logDebug(msg)
        plugin.sendToMinecraft(username, event.author.id, event.message.content)
    }

    fun onUnexpectedError(ws: WebSocket, wse: WebSocketException) {
        plugin.logger.severe("Unexpected error from DiscordBridge: ${wse.message}")
    }

    fun onDisconnected(webSocket: WebSocket, serverCloseFrame: WebSocketFrame, clientCloseFrame: WebSocketFrame, closedByServer: Boolean) {
        plugin.logDebug("Discord disconnected - attempting to reconnect")
        connection.reconnect()
    }
}
