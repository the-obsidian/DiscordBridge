package gg.obsidian.discordbridge

import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level
import org.bukkit.ChatColor
import java.io.File

class Plugin : JavaPlugin() {

    val configuration = Configuration(this)
    var connection: DiscordConnection? = null
    var users: ConfigAccessor = ConfigAccessor(this, dataFolder, "usernames.yml")
    var worlds: ConfigAccessor? = null
    var requests: MutableList<UserAlias> = mutableListOf()

    override fun onEnable() {
        updateConfig(description.version)
        users.saveDefaultConfig()
        if (isMultiverse()) worlds = ConfigAccessor(this, File("plugins/Multiverse-Core"), "worlds.yml")


        this.connection = DiscordConnection(this)

        server.scheduler.runTaskAsynchronously(this, connection)
        server.pluginManager.registerEvents(EventListener(this), this)
        getCommand("discord").executor = CommandHandler(this)
        getCommand("marina").executor = HandleMarina(this)
    }

    override fun onDisable() {
        connection!!.relay("Shutting down...")
        logger.log(Level.INFO, "Attempting to cancel tasks")
        server.scheduler.cancelTasks(this)
    }

    fun reload() {

        reloadConfig()
        users.reloadConfig()
        configuration.load()
        connection?.reconnect()
    }

    // Message senders

    fun sendToDiscordRelaySelf(message: String) {
        logDebug("Sending message to Discord - $message")
        connection!!.relay(message)
    }

    fun sendToDiscordRelay(message: String, uuid: String) {
        val alias = users.config.getString("mcaliases.$uuid.discordusername")
        var newMessage = message

        // This section should convert attempted @mentions to real ones wherever possible
        val discordusers = getDiscordUsers()
        val discordaliases: MutableList<Pair<String, String>> = mutableListOf()
        discordusers
                .filter { users.config.isSet("discordaliases.${it.second}") }
                .mapTo(discordaliases) {
                    Pair(users.config.getString("discordaliases.${it.second}.mcusername"),
                            it.second)
                }
        for (match in Regex("""(?:^| )@(w+)""").findAll(message)) {
            val found: Triple<String, String, Boolean>? = discordusers.firstOrNull {
                it.first.replace("\\s+", "").toLowerCase() == match.value.toLowerCase()
            }
            if (found != null) newMessage = newMessage.replaceFirst("@${match.value}", "<@${found.second}>")

            val found2: Pair<String, String>? = discordaliases.firstOrNull {
                it.second.toLowerCase() == match.value.toLowerCase()
            }
            if (found2 != null) newMessage = newMessage.replaceFirst("@${match.value}", "<@${found2.first}>")
        }

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
        var alias = users.config.getString("discordaliases.$id.mcusername")
        if (alias == null) alias = username.replace("\\s+", "")
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

    fun sendToMinecraftBroadcast(message: String) {
        val formattedMessage = Util.formatMessage(
                configuration.TEMPLATES_MINECRAFT_CHAT_MESSAGE,
                mapOf(
                        "%u" to configuration.USERNAME_COLOR + configuration.USERNAME.replace("\\s+", "") + "&r",
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

    fun isMultiverse(): Boolean {
        return server.pluginManager.getPlugin("Multiverse-Core") != null
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
                " account. If this is you, respond '<@me> confirm'. If this is not" +
                " you, respond '<@me> deny'."
        connection!!.tell(msg, ua.discordId)
    }

    fun getDiscordUsers(): List<Triple<String, String, Boolean>> {
        return connection!!.listUsers()
    }

    fun getDiscordOnline(): List<Triple<String, Boolean, OnlineStatus>> {
        return connection!!.listOnline()
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
