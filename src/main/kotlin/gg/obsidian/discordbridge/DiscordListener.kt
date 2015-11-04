package gg.obsidian.discordbridge

import me.itsghost.jdiscord.DiscordAPI
import me.itsghost.jdiscord.event.EventListener
import me.itsghost.jdiscord.events.UserChatEvent
import org.bukkit.ChatColor

class DiscordListener(val plugin: DiscordBridge, val api: DiscordAPI) : EventListener {

    fun userChat(e: UserChatEvent) {
        if (!e.server.id.equals(plugin.serverID)) {
            return
        }

        if (!e.group.name.equals(plugin.channel, true)) {
            return
        }

        val username: String = e.user.user.username

        if (username.equals(plugin.username, true)) {
            return
        }

        val broadcastMessage = "<" + username + ChatColor.AQUA + "(discord)" + ChatColor.RESET + "> " + e.msg.message

        plugin.server.broadcastMessage(broadcastMessage)
    }
}
