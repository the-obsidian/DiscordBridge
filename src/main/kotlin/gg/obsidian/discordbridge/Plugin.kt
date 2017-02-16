package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.Utils.Utils
import gg.obsidian.discordbridge.discord.Connection
import gg.obsidian.discordbridge.minecraft.commands.Discord
import gg.obsidian.discordbridge.minecraft.commands.Marina
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level
import java.io.File

class Plugin : JavaPlugin() {

    val cfg = Configuration(this)
    var connection: Connection? = null
    var users: DataConfigAccessor = DataConfigAccessor(this, dataFolder, "usernames.yml")
    var worlds: DataConfigAccessor? = null
    var requests: MutableList<UserAlias> = mutableListOf()

    override fun onEnable() {
        // Load configs
        updateConfig(description.version)
        users.saveDefaultConfig()
        if (isMultiverse()) worlds = DataConfigAccessor(this, File("plugins/Multiverse-Core"), "worlds.yml")

        // Connect to Discord
        this.connection = Connection(this)
        server.scheduler.runTaskAsynchronously(this, connection)
        server.pluginManager.registerEvents(EventListener(this), this)

        // Register commands
        getCommand("discord").executor = Discord(this)
        getCommand("marina").executor = Marina(this)
    }

    override fun onDisable() {
        connection!!.relay("Shutting down...")

        // Pretend like this does anything
        logger.log(Level.INFO, "Attempting to cancel tasks")
        server.scheduler.cancelTasks(this)
    }

    /*======================================
      Messaging Functions
    ===================================== */

    // Send a message to Discord as the bot itself
    fun sendToDiscord(message: String) {
        logDebug("Sending message to Discord - $message")
        connection!!.relay(message)
    }

    // Send a message from a Minecraft player to Discord
    fun sendToDiscord(message: String, uuid: String) {
        val alias = users.data.getString("mcaliases.$uuid.discordusername")
        var newMessage = message

        // This gross section converts attempted @mentions to real ones wherever possible
        // Mentionable names MUST not contain spaces!
        val discordusers = getDiscordUsers()
        val discordaliases: MutableList<Pair<String, String>> = mutableListOf()
        discordusers
                .filter { users.data.isSet("discordaliases.${it.second}") }
                .mapTo(discordaliases) {
                    Pair(users.data.getString("discordaliases.${it.second}.mcusername"),
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

        if (alias != null) newMessage = message.replaceFirst(users.data.getString("mcaliases.$uuid.mcusername"), alias)
        logDebug("Sending message to Discord - $newMessage")
        connection!!.relay(newMessage)
    }

    // TODO: this function is  little different because it isn't a simple relay. Consider refactoring.
    // Send a direct message to a Discord user as the bot itself
    fun sendToDiscord(message: String, event: MessageReceivedEvent) {
        if (event.isFromType(ChannelType.PRIVATE)) logDebug("Sending message to ${event.author.name} - $message")
        else logDebug("Sending message to Discord - $message")
        connection!!.respond(message, event)
    }

    // Send a message to Minecraft as the bot iself
    fun sendToMinecraft(message: String) {
        val formattedMessage = Utils.formatMessage(
                cfg.TEMPLATES_MINECRAFT_CHAT_MESSAGE,
                mapOf(
                        "%u" to cfg.USERNAME_COLOR + cfg.USERNAME.replace("\\s+", "") + "&r",
                        "%m" to message
                ),
                colors = true
        )
        server.broadcastMessage(formattedMessage)
    }

    // Send a message to Minecraft from a Discord user
    fun sendToMinecraft(message: String, username: String, id: String) {
        var alias = users.data.getString("discordaliases.$id.mcusername")
        if (alias == null) alias = username.replace("\\s+", "")
        val formattedMessage = Utils.formatMessage(
                cfg.TEMPLATES_MINECRAFT_CHAT_MESSAGE,
                mapOf("%u" to alias, "%m" to message),
                colors = true
        )
        server.broadcastMessage(formattedMessage)
    }

    /*===========================================
      Util
    ===========================================*/

    // Reload configs
    fun reload() {
        reloadConfig()
        users.reloadConfig()
        if (isMultiverse()) worlds!!.reloadConfig()
        cfg.load()
        //connection?.reconnect()
    }

    // Save default config
    fun updateConfig(version: String) {
        this.saveDefaultConfig()
        config.options().copyDefaults(true)
        config.set("version", version)
        saveConfig()
        cfg.load()
    }

    // Log only if debug config is true
    fun logDebug(msg: String) {
        if (!cfg.DEBUG) return
        logger.info(msg)
    }

    // Shorthand function to check if Multiverse is installed
    fun isMultiverse(): Boolean {
        return server.pluginManager.getPlugin("Multiverse-Core") != null
    }

    // Get a list of usernames of players who are online
    fun getOnlinePlayers(): List<String> {
        val names: MutableList<String> = mutableListOf()
        val players = server.onlinePlayers.toTypedArray()
        players.mapTo(names) { it.name }
        return names.toList()
    }

    // Open a request to link a Minecraft user with a Discord user
    fun registerUserRequest(ua: UserAlias) {
        requests.add(ua)
        val msg = "Minecraft user '${ua.mcUsername}' has requested to become associated with your Discord" +
                " account. If this is you, respond '<@me> confirm'. If this is not" +
                " you, respond '<@me> deny'."
        connection!!.tell(msg, ua.discordId)
    }

    // Return a list of all Discord users in the specified server
    fun getDiscordUsers(): List<Triple<String, String, Boolean>> {
        return connection!!.listUsers()
    }

    // Return a list of all Discord users in the specified server who are visibly available
    fun getDiscordOnline(): List<Triple<String, Boolean, OnlineStatus>> {
        return connection!!.listOnline()
    }

    // TODO: Rename function to something more intuitive
    // Add an alias to the Users data
    fun updateAlias(ua: UserAlias) {
        users.data.set("mcaliases.${ua.mcUuid}.mcusername", ua.mcUsername)
        users.data.set("mcaliases.${ua.mcUuid}.discordusername", ua.discordUsername)
        users.data.set("mcaliases.${ua.mcUuid}.discordid", ua.discordId)
        users.data.set("discordaliases.${ua.discordId}.mcuuid", ua.mcUuid)
        users.data.set("discordaliases.${ua.discordId}.mcusername", ua.mcUsername)
        users.data.set("discordaliases.${ua.discordId}.discordusername", ua.discordUsername)
        users.saveConfig()
    }
}
