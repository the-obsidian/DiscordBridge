package gg.obsidian.discordbridge.discord

import gg.obsidian.discordbridge.DiscordBridge
import gg.obsidian.discordbridge.util.enum.Cfg
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.*

/**
 * Maintains the connection to Discord's servers. All calls to and from the Discord API are passed through this object.
 *
 * This object is designed to run asynchronously on its own thread.
 */
object Connection: Runnable {
    lateinit var JDA: JDA
    private lateinit var listener: Listener
    var server: Guild? = null
    var channel: TextChannel? = null
    var serverReady = false
    var jdaReady = false

    /**
     * Starts the connection
     */
    override fun run() {
        try {
            DiscordBridge.logger.info("Connecting...")
            connect()
        } catch (e: Exception) {
            DiscordBridge.logger.severe("Error connecting to Discord: ${e.message}", e)
        }
    }

    /**
     * Gets the channel instance of the designated relay channel specified in the config
     *
     * @return the relay TextChannel, null if the value could not be read
     */
    fun getRelayChannel(): TextChannel? {
        server = if (server == null) getServerById(DiscordBridge.getConfig(Cfg.CONFIG).getString("server-id", "")) else server
        channel = if (channel == null) getGroupByName(server!!, DiscordBridge.getConfig(Cfg.CONFIG).getString("channel", "")) else channel
        return channel
    }

    /**
     * Sends a message to Discord
     *
     * @param message the message to send
     * @param toChannel the channel to send the message to
     */
    fun send(message: String, toChannel: MessageChannel?) {
        if (toChannel == null) {
            DiscordBridge.logger.severe("Could not send message to Discord: Channel is not defined")
            return
        }
        toChannel.sendMessage(message).queue()
    }

    fun disconnect() {
        JDA.removeEventListener(listener)
        server = null
        channel = null
        JDA.shutdown()
        jdaReady = false
    }

    /**
     * Shuts down the current JDA instance and creates a new one
     */
    fun reconnect(callback: Runnable) {
        if (DiscordBridge.getConfig(Cfg.CONFIG).getBoolean("announce-server-start-stop", true))
            send("Refreshing Discord connection...", getRelayChannel())
        disconnect()
        connect()
        callback.run()
    }

    /**
     * Gets a list of users in the relay channel
     *
     * @return a List of Member objects
     */
    fun listUsers(): List<Member> {
        channel = getRelayChannel()
        if (channel == null) return mutableListOf()
        return channel!!.members
    }

    /**
     * Gets a list of members in the relay channel who are not offline
     *
     * @return a List of Member objects
     */
    fun listOnline(): List<Member> {
        channel = getRelayChannel()
        if (channel != null) return channel!!.members
        return mutableListOf()
    }

    fun onServerReady() {
        //TODO: Fix this race condition
        if (jdaReady) send(DiscordBridge.getConfig(Cfg.CONFIG).getString("templates.discord.server-start", "Server started!"), getRelayChannel())
        serverReady = true
    }

    /**
     * Builds a JDA instance to connect to the Discord API
     */
    private fun connect() {
        var builder = JDABuilder(AccountType.BOT).setAudioEnabled(true)
        builder = builder.setToken(DiscordBridge.getConfig(Cfg.CONFIG).getString("token", ""))
        JDA = builder.buildBlocking()
        listener = Listener()
        JDA.addEventListener(listener)
        JDA.presence.game = Game.of("Minecraft ${DiscordBridge.getServer().getMinecraftVersion()}")
        if (serverReady) send(DiscordBridge.getConfig(Cfg.CONFIG).getString("templates.discord.server-start", "Server started!"), getRelayChannel())
        jdaReady = true
    }

    /**
     * Fetches a text channel given its Guild and its name
     *
     * @param server the Guild object of the server the channel is on
     * @param name the name of the channel
     * @return the channel's TextChannel object, or null if it couldn't be found
     */
    private fun getGroupByName(server: Guild, name: String): TextChannel? {
        return server.textChannels.firstOrNull { it.name == name }
    }

    /**
     * Fetches a server given its unique ID
     *
     * @param id the unique ID of the server
     * @return the server's Guild object, or null if it couldn't be found
     */
    private fun getServerById(id: String): Guild? {
        return JDA.guilds.firstOrNull { it.id.equals(id, true) }
    }
}
