package gg.obsidian.discordbridge.discord

import gg.obsidian.discordbridge.Plugin
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.TextChannel

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

    // TODO: Try to merge this into "send"
    fun tell(message: String, id: String) {
        val modifiedMsg = message.replace("<@me>", api!!.selfUser.asMention)
        api!!.getUserById(id).privateChannel.sendMessage(modifiedMsg).queue()
    }

    fun reconnect() {
        disconnect()
        connect()
    }

    // TODO: Unfuck this
    fun listUsers(): List<Triple<String, String, Boolean>> {
        channel = if (channel == null) getGroupByName(server!!, plugin.cfg.CHANNEL) else channel
        if (channel == null) return mutableListOf()

        val listOfUsers: MutableList<Triple<String, String, Boolean>> = mutableListOf()
        channel!!.members.mapTo(listOfUsers) {
            Triple(it.effectiveName, it.user.id, it.user.isBot)
        }
        return listOfUsers
    }

    // TODO: Unfuck this
    fun listOnline(): List<Triple<String, Boolean, OnlineStatus>> {
        channel = if (channel == null) getGroupByName(server!!, plugin.cfg.CHANNEL) else channel
        if (channel == null) return mutableListOf()

        val listOfUsers: MutableList<Triple<String, Boolean, OnlineStatus>> = mutableListOf()
        channel!!.members.mapTo(listOfUsers) {
            Triple(it.effectiveName, it.user.isBot, it.onlineStatus)
        }
        return listOfUsers
    }

    private fun disconnect() {
        api?.removeEventListener(listener)
        api?.shutdown(false)
    }

    private fun connect() {
        var builder = JDABuilder(AccountType.BOT).setAudioEnabled(false)
        builder = builder.setToken(plugin.cfg.TOKEN)
        api = builder.buildBlocking()
        listener = Listener(plugin, api as JDA, this)
        api!!.addEventListener(listener)
        if(plugin.cfg.ANNOUNCE_SERVER_START_STOP)
            send(plugin.cfg.TEMPLATES_DISCORD_SERVER_START, getRelayChannel())
    }

    private fun getServerById(id: String): Guild? {
        return api!!.guilds.firstOrNull { it.id.equals(id, true) }
    }

    private fun getGroupByName(server: Guild, name: String): TextChannel? {
        return server.textChannels.firstOrNull { it.name == name }
    }
}
