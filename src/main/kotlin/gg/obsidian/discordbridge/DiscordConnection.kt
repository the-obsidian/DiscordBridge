package gg.obsidian.discordbridge

import net.dv8tion.jda.JDA
import net.dv8tion.jda.JDABuilder
import net.dv8tion.jda.entities.Guild
import net.dv8tion.jda.entities.TextChannel

class DiscordConnection(val plugin: Plugin) : Runnable {
    var api: JDA? = null
    var listener: DiscordListener? = null
    var server: Guild? = null
    var channel: TextChannel? = null

    override fun run() {
        try {
            connect()
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
        disconnect()
        connect()
    }

    private fun disconnect() {
        api?.removeEventListener(listener)
        api?.shutdown(false)
    }

    private fun connect() {
        var builder = JDABuilder().setAudioEnabled(false)
        if (plugin.configuration.TOKEN != "") {
            builder = builder.setBotToken(plugin.configuration.TOKEN)
        } else {
            builder = builder.setEmail(plugin.configuration.EMAIL).setPassword(plugin.configuration.PASSWORD)
        }
        api = builder.buildBlocking()
        listener = DiscordListener(plugin, api as JDA, this)
        api!!.addEventListener(listener)
    }

    private fun getServerById(id: String): Guild? {
        for (server in api!!.guilds)
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
