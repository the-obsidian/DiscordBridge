package gg.obsidian.discordbridge

import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

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

    fun relay(message: String) {
        server = if (server == null) getServerById(plugin.configuration.SERVER_ID) else server
        if (server == null) return

        channel = if (channel == null) getGroupByName(server!!, plugin.configuration.CHANNEL) else channel
        if (channel == null) return

        channel!!.sendMessage(message).queue()
    }

    fun respond(message: String, event: MessageReceivedEvent) {
        if (event.isFromType(ChannelType.PRIVATE)) event.privateChannel.sendMessage(message).queue()
        else event.channel.sendMessage(message).queue()
    }

    fun tell(message: String, id: String) {
        api!!.getUserById(id).privateChannel.sendMessage(message).queue()
    }

    fun reconnect() {
        disconnect()
        connect()
    }

    fun listUsers(): List<Pair<String, String>> {
        channel = if (channel == null) getGroupByName(server!!, plugin.configuration.CHANNEL) else channel
        if (channel == null) return mutableListOf()

        val listOfUsers: MutableList<Pair<String, String>> = mutableListOf()
        channel!!.members.mapTo(listOfUsers) { Pair(it.effectiveName, it.user.id) }
        return listOfUsers
    }

    private fun disconnect() {
        api?.removeEventListener(listener)
        api?.shutdown(false)
    }

    private fun connect() {
        var builder = JDABuilder(AccountType.BOT).setAudioEnabled(false)
        builder = builder.setToken(plugin.configuration.TOKEN)
        api = builder.buildBlocking()
        listener = DiscordListener(plugin, api as JDA, this)
        api!!.addEventListener(listener)
    }

    private fun getServerById(id: String): Guild? {
        return api!!.guilds.firstOrNull { it.id.equals(id, true) }
    }

    private fun getGroupByName(server: Guild, name: String): TextChannel? {
        return server.textChannels.firstOrNull { it.name == name }
    }
}
