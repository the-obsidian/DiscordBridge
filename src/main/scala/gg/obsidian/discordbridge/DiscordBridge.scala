package gg.obsidian.discordbridge

import org.bukkit.event.{EventHandler, EventPriority, Listener}
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.plugin.java.JavaPlugin

class DiscordBridge extends JavaPlugin with Listener {

  var serverID: String = _
  var channel: String = _
  var username: String = _
  var email: String = _
  var password: String = _
  var connection: DiscordConnection = _

  override def onEnable() = {
    updateConfig(getDescription().getVersion())

    this.serverID = getConfig().getString("settings.server-id")
    this.channel = getConfig().getString("settings.channel")
    this.username = getConfig().getString("settings.username")
    this.email = getConfig().getString("settings.email")
    this.password = getConfig().getString("settings.password")

    this.connection = new DiscordConnection(this)

    getServer().getScheduler().runTaskAsynchronously(this, connection);
    getServer().getPluginManager().registerEvents(this, this);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  def onChat(event: AsyncPlayerChatEvent) = {
    send(event.getPlayer().getName(), event.getMessage());
  }

  def send(name: String, message: String) = {
    connection.send(name, message)
  }

  def updateConfig(version: String) = {
    saveDefaultConfig()
    getConfig().options().copyDefaults(true)
    getConfig().set("version", version)
    saveConfig()
  }
}