package gg.obsidian.discordbridge

import net.dv8tion.jda.JDABuilder
import net.dv8tion.jda.entities.Guild
import net.dv8tion.jda.entities.TextChannel

class DiscordConnection(val plugin: Plugin) : Runnable {
    var api = JDABuilder(plugin.configuration.EMAIL, plugin.configuration.PASSWORD).enableVoice(false).buildBlocking()
    var listener = DiscordListener(plugin, api, this)
    var server: Guild? = null
    var channel: TextChannel? = null

    override fun run() {
        try {
            api.addEventListener(listener)
        } catch (e: Exception) {
            plugin.logger.severe("Error connecting to Discord: " + e)
        }

    }

    fun send(message: String) {
        server = if (server == null) getServerById(plugin.configuration.SERVER_ID) else server
        if (server == null) return

        channel = if (channel == null) getGroupByName(server!!, plugin.configuration.CHANNEL) else channel
        if (channel == null) return

        channel!!.sendMessage(message)
    }

    fun reconnect() {
        api.removeEventListener(listener)
        api.shutdown(false)
        api = JDABuilder(plugin.configuration.EMAIL, plugin.configuration.PASSWORD).build()
        listener = DiscordListener(plugin, api, this)
        api.addEventListener(listener)
    }

    private fun getServerById(id: String): Guild? {
        for (server in api.guilds)
            if (server.id.equals(id, true))
                return server
        return null
    }

    private fun getGroupByName(server: Guild, name: String): TextChannel? {
        for (group in server.textChannels)
            if (group.name.equals(name))
                return group
        return null
    }
}
