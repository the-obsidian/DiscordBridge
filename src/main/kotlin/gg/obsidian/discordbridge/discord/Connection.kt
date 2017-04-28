package gg.obsidian.discordbridge.discord

import gg.obsidian.discordbridge.Config
import gg.obsidian.discordbridge.Plugin
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.*

object Connection: Runnable {
    lateinit var plugin: Plugin
    lateinit var JDA: JDA
    lateinit var listener: Listener
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
        server = if (server == null) getServerById(Config.SERVER_ID) else server
        channel = if (channel == null) getGroupByName(server!!, Config.CHANNEL) else channel
        return channel
    }

    fun send(message: String, toChannel: MessageChannel?) {
        if (toChannel == null) {
            plugin.logger.severe("Could not send message to Discord: Channel is not defined")
            return
        }
        toChannel.sendMessage(message).queue()
    }

    fun reconnect(callback: Runnable) {
        //disconnect
        if (Config.ANNOUNCE_SERVER_START_STOP)
            send("Refreshing Discord connection...", getRelayChannel())
        JDA.removeEventListener(listener)
        server = null
        channel = null
        JDA.shutdown(false)

        //reconnect
        connect()
        callback.run()
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
        var builder = JDABuilder(AccountType.BOT).setAudioEnabled(true)
        builder = builder.setToken(Config.TOKEN)
        JDA = builder.buildBlocking()
        listener = Listener(plugin)
        JDA.addEventListener(listener)
        if (Config.ANNOUNCE_SERVER_START_STOP)
            send(Config.TEMPLATES_DISCORD_SERVER_START, getRelayChannel())
        JDA.presence.game = Game.of("Minecraft ${plugin.server.bukkitVersion.split("-")[0]}")
    }

    private fun getGroupByName(server: Guild, name: String): TextChannel? {
        return server.textChannels.firstOrNull { it.name == name }
    }

    private fun getServerById(id: String): Guild? {
        return JDA.guilds.firstOrNull { it.id.equals(id, true) }
    }
}
