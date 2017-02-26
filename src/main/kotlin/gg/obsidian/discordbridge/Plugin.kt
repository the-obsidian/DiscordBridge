package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.discord.Connection
import gg.obsidian.discordbridge.minecraft.commands.Discord
import gg.obsidian.discordbridge.minecraft.commands.Marina
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.MessageChannel
import org.bukkit.ChatColor
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level
import java.io.File

class Plugin : JavaPlugin() {

    val cfg = Configuration(this)
    lateinit var connection: Connection
    var users: DataConfigAccessor = DataConfigAccessor(this, dataFolder, "usernames.yml")
    var memory: DataConfigAccessor = DataConfigAccessor(this, dataFolder, "botmemory.yml")
    var scripted_responses: DataConfigAccessor = DataConfigAccessor(this, dataFolder, "scriptedresponses.yml")
    var worlds: DataConfigAccessor? = null
    var requests: MutableList<UserAlias> = mutableListOf()

    override fun onEnable() {
        // Load configs
        updateConfig(description.version)
        users.saveDefaultConfig()
        memory.saveDefaultConfig()
        scripted_responses.saveDefaultConfig()
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
        if (cfg.ANNOUNCE_SERVER_START_STOP)
            connection.send(cfg.TEMPLATES_DISCORD_SERVER_STOP, connection.getRelayChannel())

        // Pretend like this does anything
        logger.log(Level.INFO, "Attempting to cancel tasks")
        server.scheduler.cancelTasks(this)
    }

    /*======================================
      Messaging Functions
    ===================================== */

    // Send a message to Discord
    fun sendToDiscord(message: String, channel: MessageChannel?) {
        logDebug("Sending message to Discord - $message")
        connection.send(message, channel)
    }

    // Send a message to Minecraft
    fun sendToMinecraft(message: String) {
        server.broadcastMessage(message)
    }

    /*===========================================
      Util
    ===========================================*/

    // Reload configs
    fun reload() {
        reloadConfig()
        users.reloadConfig()
        memory.reloadConfig()
        scripted_responses.reloadConfig()
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
        connection.tell(msg, ua.discordId)
    }

    // Return a list of all Discord users in the specified server
    fun getDiscordUsers(): List<Triple<String, String, Boolean>> {
        return connection.listUsers()
    }

    // Return a list of all Discord users in the specified server who are visibly available
    fun getDiscordOnline(): List<Triple<String, Boolean, OnlineStatus>> {
        return connection.listOnline()
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

    /*======================================
      Message Formatting Functions
    ===================================== */

    // Converts attempted @mentions to real ones wherever possible
    // Replaces the sender's name with their registered alias if it exists
    // Mentionable names MUST NOT contain spaces!
    fun convertAtMentions(message: String): String {
        var newMessage = message

        val discordusers = getDiscordUsers()
        val discordaliases: MutableList<Pair<String, String>> = mutableListOf()
        discordusers
                .filter { users.data.isSet("discordaliases.${it.second}") }
                .mapTo(discordaliases) { Pair(users.data.getString("discordaliases.${it.second}.mcusername"), it.second) }
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

        return newMessage
    }

    // Scans the string for occurrences of the Minecraft name matching the given UUID and attempts to translate it
    // to a registered Discord name, if it exists
    fun translateAliasToDiscord(message: String, uuid: String?): String {
        var newMessage = message
        val alias = users.data.getString("mcaliases.$uuid.discordusername")
        if (alias != null)
            newMessage = newMessage.replaceFirst(users.data.getString("mcaliases.$uuid.mcusername"), alias)
        return newMessage
    }

    fun toMinecraftChatMessage(message: String, alias: String): String {
        var formattedString = cfg.TEMPLATES_MINECRAFT_CHAT_MESSAGE
        formattedString = formattedString.replace("%u", alias).replace("%m", message)
        formattedString = ChatColor.translateAlternateColorCodes('&', formattedString)
        return formattedString
    }

    fun toDiscordChatMessage(message: String, username: String, displayName: String, worldName: String): String {
        var formattedString = cfg.TEMPLATES_DISCORD_CHAT_MESSAGE
        formattedString = formattedString.replace("%u", username).replace("%m", message)
                .replace("%d", displayName).replace("%w", worldName)
        formattedString = ChatColor.translateAlternateColorCodes('&', formattedString)
        return formattedString
    }

    fun toDiscordPlayerJoin(username: String, displayName: String): String {
        var formattedString = cfg.TEMPLATES_DISCORD_PLAYER_JOIN
        formattedString = formattedString.replace("%u", username).replace("%d", displayName)
        formattedString = ChatColor.translateAlternateColorCodes('&', formattedString)
        return formattedString
    }

    fun toDiscordPlayerLeave(username: String, displayName: String): String {
        var formattedString = cfg.TEMPLATES_DISCORD_PLAYER_LEAVE
        formattedString = formattedString.replace("%u", username).replace("%d", displayName)
        formattedString = ChatColor.translateAlternateColorCodes('&', formattedString)
        return formattedString
    }

    fun toDiscordPlayerDeath(deathMessage: String, username: String, displayName: String, worldName: String): String {
        var formattedString = cfg.TEMPLATES_DISCORD_PLAYER_DEATH
        formattedString = formattedString.replace("%u", username).replace("%r", deathMessage)
                .replace("%d", displayName).replace("%w", worldName)
        formattedString = ChatColor.translateAlternateColorCodes('&', formattedString)
        return formattedString
    }
}
