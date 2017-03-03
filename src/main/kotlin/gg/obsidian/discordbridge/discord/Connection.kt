package gg.obsidian.discordbridge.discord

import gg.obsidian.discordbridge.Plugin
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.*

class Connection(val plugin: Plugin) : Runnable {
    var api: JDA? = null
    var listener: Listener? = null
    var server: Guild? = null
    var channel: TextChannel? = null

    override fun run() {
        try {
            connect()
        } catch (e: Exception) {
            plugin.logger.severe("Error connecting to Discord: " + e)
        }
    }

    fun getRelayChannel(): TextChannel? {
        server = if (server == null) getServerById(plugin.cfg.SERVER_ID) else server
        channel = if (channel == null) getGroupByName(server!!, plugin.cfg.CHANNEL) else channel
        return channel
    }

    fun send(message: String, toChannel: MessageChannel?) {
        if (toChannel == null) {
            plugin.logger.severe("Could not send message to Discord: Channel is not defined")
            return
        }
        toChannel.sendMessage(message).queue()
    }

    fun reconnect() {
        disconnect()
        connect()
    }

    fun listUsers(): List<Member> {
        channel = getRelayChannel()
        if (channel == null) return mutableListOf()
        return channel!!.members
    }

    fun listOnline(): List<Member> {
        channel = getRelayChannel()
        if (channel != null) return channel!!.members
        return mutableListOf()
    }

    private fun connect() {
        var builder = JDABuilder(AccountType.BOT).setAudioEnabled(false)
        builder = builder.setToken(plugin.cfg.TOKEN)
        api = builder.buildBlocking()
        listener = Listener(plugin, api as JDA, this)
        api!!.addEventListener(listener)
        if (plugin.cfg.ANNOUNCE_SERVER_START_STOP)
            send(plugin.cfg.TEMPLATES_DISCORD_SERVER_START, getRelayChannel())
        api!!.presence.game = Game.of("Minecraft ${plugin.server.bukkitVersion.split("-")[0]}")
    }

    private fun disconnect() {
        api?.removeEventListener(listener)
        api?.shutdown(false)
    }

    private fun getGroupByName(server: Guild, name: String): TextChannel? {
        return server.textChannels.firstOrNull { it.name == name }
    }

    private fun getServerById(id: String): Guild? {
        return api!!.guilds.firstOrNull { it.id.equals(id, true) }
    }
}
