package gg.obsidian.discordbridge.discord

import gg.obsidian.discordbridge.Plugin
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

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

    fun relay(message: String) {
        server = if (server == null) getServerById(plugin.cfg.SERVER_ID) else server
        if (server == null) return

        channel = if (channel == null) getGroupByName(server!!, plugin.cfg.CHANNEL) else channel
        if (channel == null) return

        channel!!.sendMessage(message).queue()
    }

    fun respond(message: String, event: MessageReceivedEvent) {
        if (event.isFromType(ChannelType.PRIVATE)) event.privateChannel.sendMessage(message).queue()
        else event.channel.sendMessage(message).queue()
    }

    fun tell(message: String, id: String) {
        val modifiedMsg = message.replace("<@me>", api!!.selfUser.asMention)
        api!!.getUserById(id).privateChannel.sendMessage(modifiedMsg).queue()
    }

    fun reconnect() {
        disconnect()
        connect()
    }

    fun listUsers(): List<Triple<String, String, Boolean>> {
        channel = if (channel == null) getGroupByName(server!!, plugin.cfg.CHANNEL) else channel
        if (channel == null) return mutableListOf()

        val listOfUsers: MutableList<Triple<String, String, Boolean>> = mutableListOf()
        channel!!.members.mapTo(listOfUsers) {
            Triple(it.effectiveName, it.user.id, it.user.isBot)
        }
        return listOfUsers
    }

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
        relay("Oikos!")
    }

    private fun getServerById(id: String): Guild? {
        return api!!.guilds.firstOrNull { it.id.equals(id, true) }
    }

    private fun getGroupByName(server: Guild, name: String): TextChannel? {
        return server.textChannels.firstOrNull { it.name == name }
    }
}
