package gg.obsidian.discordbridge

import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.bukkit.plugin.java.JavaPlugin

class Plugin : JavaPlugin() {

    val configuration = Configuration(this)
    var connection: DiscordConnection? = null
    var users: ConfigAccessor = ConfigAccessor(this, "usernames.yml")
    var requests: MutableList<UserAlias> = mutableListOf()

    override fun onEnable() {
        updateConfig(description.version)

        this.connection = DiscordConnection(this)

        server.scheduler.runTaskAsynchronously(this, connection)
        server.pluginManager.registerEvents(EventListener(this), this)
        getCommand("discord").executor = CommandHandler(this)
        getCommand("registeralias").executor = CommandHandler(this)
        getCommand("getdiscordids").executor = CommandHandler(this)
    }

    override fun onDisable() {
        server.scheduler.cancelTasks(this)
    }

    fun reload() {
        reloadConfig()
        users.reloadConfig()
        configuration.load()
        connection?.reconnect()
    }

    // Message senders

    fun sendToDiscordRelay(message: String, uuid: String) {
        val alias = users.config.getString("mcaliases.$uuid.discordusername")
        var newMessage = message
        if (alias != null) newMessage = message.replaceFirst(users.config.getString("mcaliases.$uuid.mcusername"), alias)
        logDebug("Sending message to Discord - $newMessage")
        connection!!.relay(newMessage)
    }

    fun sendToDiscordRespond(message: String, event: MessageReceivedEvent) {
        if (event.isFromType(ChannelType.PRIVATE)) logDebug("Sending message to ${event.author.name} - $message")
        else logDebug("Sending message to Discord - $message")
        connection!!.respond(message, event)
    }

    fun sendToMinecraft(username: String, id: String, message: String) {
        var alias = users.config.getString("discordAliases.$id.mcusername")
        if (alias == null) alias = username
        val formattedMessage = Util.formatMessage(
                configuration.TEMPLATES_MINECRAFT_CHAT_MESSAGE,
                mapOf(
                        "%u" to alias,
                        "%m" to message
                ),
                colors = true
        )

        server.broadcastMessage(formattedMessage)
    }

    // Utilities

    fun updateConfig(version: String) {
        this.saveDefaultConfig()
        config.options().copyDefaults(true)
        config.set("version", version)
        saveConfig()
        configuration.load()
    }

    fun logDebug(msg: String) {
        if (!configuration.DEBUG) return
        logger.info(msg)
    }

    // Stuff

    fun getOnlinePlayers(): List<String> {
        val names: MutableList<String> = mutableListOf()
        val players = server.onlinePlayers.toTypedArray()
        players.mapTo(names) { it.name }
        return names.toList()
    }

    fun registerUserRequest(ua: UserAlias) {
        requests.add(ua)
        val msg = "Minecraft user '${ua.mcUsername}' has requested to become associated with your Discord" +
                " account. If this is you, respond '<@267902537074606082> confirm'. If this is not" +
                " you, respond '<@267902537074606082> deny'."
        connection!!.tell(msg, ua.discordId)
    }

    fun getDiscordUsers(): List<Pair<String, String>> {
        return connection!!.listUsers()
    }

    fun updateAlias(ua: UserAlias) {
        users.config.set("mcaliases.${ua.mcUuid}.mcusername", ua.mcUsername)
        users.config.set("mcaliases.${ua.mcUuid}.discordusername", ua.discordUsername)
        users.config.set("mcaliases.${ua.mcUuid}.discordid", ua.discordId)
        users.config.set("discordaliases.${ua.discordId}.mcuuid", ua.mcUuid)
        users.config.set("discordaliases.${ua.discordId}.mcusername", ua.mcUsername)
        users.config.set("discordaliases.${ua.discordId}.discordusername", ua.discordUsername)
        users.saveConfig()
    }
}
