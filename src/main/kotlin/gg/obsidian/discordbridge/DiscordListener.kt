package gg.obsidian.discordbridge

import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketException
import net.dv8tion.jda.JDA
import net.dv8tion.jda.events.message.MessageReceivedEvent
import net.dv8tion.jda.hooks.ListenerAdapter

class DiscordListener(val plugin: Plugin, val api: JDA) : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        plugin.logDebug("Received message ${event.message.id} from Discord")

        if (!event.guild.id.equals(plugin.configuration.SERVER_ID)) {
            plugin.logDebug("Ignoring message ${event.message.id} from Discord: server does not match")
            return
        }

        if (!event.textChannel.name.equals(plugin.configuration.CHANNEL, true)) {
            plugin.logDebug("Ignoring message ${event.message.id} from Discord: channel does not match")
            return
        }

        val username: String = event.author.username

        if (username.equals(plugin.configuration.USERNAME, true)) {
            plugin.logDebug("Ignoring message ${event.message.id} from Discord: it matches the server's username")
            return
        }

        plugin.logDebug("Broadcasting message ${event.message.id} from Discord as user $username")
        plugin.sendToMinecraft(username, event.message.content)
    }

    fun onUnexpectedError(ws: WebSocket, wse: WebSocketException) {
        plugin.logger.severe("Unexpected error from DiscordBridge: ${wse.message}")
    }
}
