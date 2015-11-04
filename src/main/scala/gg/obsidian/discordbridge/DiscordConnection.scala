package gg.obsidian.discordbridge

import me.itsghost.jdiscord.talkable.Group
import me.itsghost.jdiscord.{DiscordAPI, DiscordBuilder, Server}

import scala.collection.JavaConversions._

class DiscordConnection(plugin: DiscordBridge) extends Runnable {

  var api: DiscordAPI = _
  var server: Option[Server] = _
  var channel: Option[Group] = _

  def run() = {
    try {
      api = new DiscordBuilder(plugin.email, plugin.password).build().login()
      api.getEventManager().registerListener(new DiscordListener(plugin, api))
    } catch {
      case e: Exception => plugin.getLogger().severe("Error connecting to Discord: " + e)
    }
  }

  def send(name: String, message: String): Unit = {
    server = server match {
      case None => getServerById(plugin.serverID)
      case _ => server
    }

    if (server.isEmpty) return

    channel = channel match {
      case None => getGroupByName(server.get, plugin.channel)
      case _ => channel
    }

    if (channel.isEmpty) return

    channel.get.sendMessage("<" + name + "> " + message)
  }

  def getServerById(id: String): Option[Server] = {
    api.getAvailableServers.toList.foreach { server: Server => {
      if (server.getId().equals(id)) return Some(server)
    }}
    None
  }

  def getGroupByName(server: Server, name: String): Option[Group] = {
    server.getGroups().toList.foreach { group: Group => {
      if (group.getName().equals(name)) return Some(group)
    }}
    None
  }
}
