package gg.obsidian.discordbridge

import me.itsghost.jdiscord.DiscordAPI
import me.itsghost.jdiscord.DiscordBuilder
import me.itsghost.jdiscord.Server
import me.itsghost.jdiscord.talkable.Group

class DiscordConnection(val plugin: Plugin) : Runnable {
    var api: DiscordAPI = DiscordBuilder(plugin.email, plugin.password).build()
    var server: Server? = null
    var channel: Group? = null

    override fun run() {
        try {
            api.login()
            api.eventManager.registerListener(DiscordListener(plugin, api))
        } catch (e: Exception) {
            plugin.logger.severe("Error connecting to Discord: " + e)
        }

    }

    fun send(name: String, message: String) {
        server = if (server == null) getServerById(plugin.serverID) else server
        if (server == null) return

        channel = if (channel == null) getGroupByName(server!!, plugin.channel) else channel
        if (channel == null) return

        channel!!.sendMessage("<$name> $message")
    }

    private fun getServerById(id: String): Server? {
        for (server in api.availableServers)
            if (server.id.equals(id, true))
                return server
        return null
    }

    private fun getGroupByName(server: Server, name: String): Group? {
        for (group in server.groups)
            if (group.name.equals(name))
                return group
        return null
    }
}
