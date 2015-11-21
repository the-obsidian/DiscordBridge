package gg.obsidian.discordbridge

import me.itsghost.jdiscord.DiscordAPI
import me.itsghost.jdiscord.event.EventListener
import me.itsghost.jdiscord.events.UserChatEvent
import org.bukkit.ChatColor

class DiscordListener(val plugin: Plugin, val api: DiscordAPI) : EventListener {

    fun userChat(e: UserChatEvent) {
        plugin.logDebug("Received message ${e.msg.id} from Discord")

        if (!e.server.id.equals(plugin.serverID)) {
            plugin.logDebug("Ignoring message ${e.msg.id} from Discord: server does not match")
            return
        }

        if (!e.group.name.equals(plugin.channel, true)) {
            plugin.logDebug("Ignoring message ${e.msg.id} from Discord: channel does not match")
            return
        }

        val username: String = e.user.user.username

        if (username.equals(plugin.username, true)) {
            plugin.logDebug("Ignoring message ${e.msg.id} from Discord: it matches the server's username")
            return
        }

        plugin.logDebug("Broadcasting message ${e.msg.id} from Discord as user $username")

        val broadcastMessage = "<" + username + ChatColor.AQUA + "(discord)" + ChatColor.RESET + "> " + e.msg.message
        plugin.server.broadcastMessage(broadcastMessage)
    }
}
