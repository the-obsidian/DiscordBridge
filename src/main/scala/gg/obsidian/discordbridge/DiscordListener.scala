package gg.obsidian.discordbridge

import me.itsghost.jdiscord.DiscordAPI
import me.itsghost.jdiscord.event.EventListener
import me.itsghost.jdiscord.events.UserChatEvent
import org.bukkit.ChatColor

class DiscordListener(plugin: DiscordBridge, api: DiscordAPI) extends EventListener {

  def userChat(e: UserChatEvent): Unit = {
    if (!e.getServer().getId().equals(plugin.serverID)) {
      return
    }

    if (!e.getGroup().getName().equalsIgnoreCase(plugin.channel)) {
      return
    }

    val username: String = e.getUser().getUser().getUsername();

    if (username.equalsIgnoreCase(plugin.username)) {
      return
    }

    val broadcastMessage: String = "<" +
      username +
      ChatColor.AQUA +
      "(discord)" +
      ChatColor.RESET +
      "> " +
      e.getMsg().getMessage()

    plugin.getServer().broadcastMessage(broadcastMessage)
  }
}
