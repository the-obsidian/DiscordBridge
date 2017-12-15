package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.discord.Connection
import gg.obsidian.discordbridge.minecraft.CommandListener
import gg.obsidian.discordbridge.minecraft.EventListener
import gg.obsidian.discordbridge.utils.Rating
import gg.obsidian.discordbridge.utils.Respect
import gg.obsidian.discordbridge.utils.Script
import gg.obsidian.discordbridge.utils.UserAlias
import gg.obsidian.discordbridge.utils.UtilFunctions.noSpace
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.MessageChannel
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.pegdown.PegDownProcessor
import java.io.File
import java.util.logging.Level
import org.bukkit.ChatColor as CC

/**
 * The primary Plugin class that maintains the plugin's connection with Bukkit
 */
class Plugin : JavaPlugin() {

    // Configs
    var users: DataConfigAccessor = DataConfigAccessor(this, dataFolder, "usernames.yml")
    var eightball: DataConfigAccessor = DataConfigAccessor(this, dataFolder, "8ball.yml")
    var insult: DataConfigAccessor = DataConfigAccessor(this, dataFolder, "insult.yml")
    var f: DataConfigAccessor = DataConfigAccessor(this, dataFolder, "f.yml")
    var rate: DataConfigAccessor = DataConfigAccessor(this, dataFolder, "rate.yml")
    var script: DataConfigAccessor = DataConfigAccessor(this, dataFolder, "script.yml")
    var worlds: DataConfigAccessor? = null

    // Markdown deserializer
    var pegDownProc = PegDownProcessor()

    // Temporary storage for alias linking requests
    var requests: MutableList<UserAlias> = mutableListOf()

    /**
     * Returns whether Multiverse-Core is installed
     */
    val isMultiverseInstalled: Boolean
        get() = server.pluginManager.getPlugin("Multiverse-Core") != null

    /**
     * Runs at plugin startup
     */
    override fun onEnable() {
        // Register data class types to the config deserializer
        ConfigurationSerialization.registerClass(Respect::class.java, "Respect")
        ConfigurationSerialization.registerClass(Rating::class.java, "Rating")
        ConfigurationSerialization.registerClass(Script::class.java, "Script")
        ConfigurationSerialization.registerClass(UserAlias::class.java, "UserAlias")

        // Load configs
        updateConfig(description.version)
        if (isMultiverseInstalled) worlds = DataConfigAccessor(this, File("plugins/Multiverse-Core"), "worlds.yml")

        // Connect to Discord
        Connection.plugin = this //TODO: enforce this better
        server.scheduler.runTaskAsynchronously(this, Connection)
        server.pluginManager.registerEvents(EventListener(this), this)

        // Register commands
        //TODO: automate this?
        getCommand("discord").executor = CommandListener(this)
        getCommand("f").executor = CommandListener(this)
        getCommand("rate").executor = CommandListener(this)
        getCommand("8ball").executor = CommandListener(this)
        getCommand("insult").executor = CommandListener(this)
        getCommand("choose").executor = CommandListener(this)
        getCommand("talk").executor = CommandListener(this)
        getCommand("roll").executor = CommandListener(this)
    }

    /**
     * Runs cleanup when the plugin is disabled
     */
    override fun onDisable() {
        if (Config.ANNOUNCE_SERVER_START_STOP)
            Connection.send(Config.TEMPLATES_DISCORD_SERVER_STOP, Connection.getRelayChannel())

        // Pretend like this does anything
        logger.log(Level.INFO, "Attempting to cancel tasks")
        server.scheduler.cancelTasks(this)
    }

    /*======================================
      Messaging Functions
    ===================================== */

    /**
     * Sends a message to the specified Discord channel
     *
     * @param message the message to send
     * @param channel the channel to send the message to
     */
    fun sendToDiscord(message: String, channel: MessageChannel?) {
        logDebug("Sending message to Discord - $message")
        Connection.send(message, channel)
    }

    /**
     * Broadcast a message on the Minecraft server
     *
     * @param message the message to send
     */
    fun sendToMinecraft(message: String) {
        server.broadcastMessage(message)
    }

    /*===========================================
      Util
    ===========================================*/

    /**
     * Reloads all configs and the JDA connection
     */
    fun reload(callback: Runnable) {
        reloadConfig()
        users.reloadConfig()
        eightball.reloadConfig()
        insult.reloadConfig()
        f.reloadConfig()
        rate.reloadConfig()
        script.reloadConfig()
        if (isMultiverseInstalled) worlds!!.reloadConfig()
        Config.load(this)
        UserAliasConfig.load(this)
        Connection.reconnect(callback)
    }

    /**
     * Saves all default configs where configs do not exist and reloads data from file into memory
     */
    private fun updateConfig(version: String) {
        this.saveDefaultConfig()
        config.options().copyDefaults(true)
        config.set("version", version)
        saveConfig()

        users.saveDefaultConfig()
        eightball.saveDefaultConfig()
        insult.saveDefaultConfig()
        f.saveDefaultConfig()
        rate.saveDefaultConfig()
        script.saveDefaultConfig()

        Config.load(this)
        UserAliasConfig.load(this)
    }

    /**
     * Sends a log message to console if the DEBUG flag in config.yml is true
     */
    fun logDebug(msg: String) {
        if (!Config.DEBUG) return
        logger.info("[DiscordBridge] $msg")
    }

    /**
     * @return a list of names of all players currently on the Minecraft server
     */
    fun getOnlinePlayers(): List<String> {
        val names: MutableList<String> = mutableListOf()
        val players = server.onlinePlayers.toTypedArray()
        players.mapTo(names) { it.name }
        return names.toList()
    }

    /**
     * Opens an alias link request and sends it to the target Discord user
     *
     * @param player the Minecraft player that initiated the request
     * @param discriminator the Discord username+discriminator of the target Discord user
     * @return a Discord Member object, or null if no matching member was found
     */
    fun registerUserRequest(player: Player, discriminator: String): Member? {
        val users = Connection.listUsers()
        val found: Member = users.find { it.user.name + "#" + it.user.discriminator == discriminator } ?: return null

        val ua = UserAlias(player.uniqueId, found.user.id)
        requests.add(ua)
        val msg = "Minecraft user '${server.getOfflinePlayer(ua.mcUuid).name}' has requested to become associated with your Discord" +
                " account. If this is you, respond '${Connection.JDA.selfUser.asMention} confirm'. If this is not" +
                " you, respond ${Connection.JDA.selfUser.asMention} deny'."
        val member = Connection.JDA.getUserById(ua.discordId)
        member.openPrivateChannel().queue({p -> p.sendMessage(msg).queue()})
        return found
    }

    /**
     * @return a formatted string listing the Discord IDs of all Discord users in the relay channel
     */
    fun getDiscordMembersAll(): String {
        val users = Connection.listUsers()

        if (users.isEmpty())
            return "${CC.YELLOW}No Discord members could be found. Either server is empty or an error has occurred."

        var response = "${CC.YELLOW}Discord users:"
        for (user in users) {
            response += if (user.user.isBot) "\n${CC.GOLD}- ${user.effectiveName} (Bot) | ${user.user.name}#${user.user.discriminator}${CC.RESET}"
            else "\n${CC.YELLOW}- ${user.effectiveName} | ${user.user.name}#${user.user.discriminator}${CC.RESET}"
        }
        return response.trim()
    }

    /**
     * @return a formatted string listing all Discord users in the relay channel who are online along with their statuses
     */
    fun getDiscordMembersOnline(): String {
        val onlineUsers = Connection.listOnline()
        if (onlineUsers.isEmpty())
            return "${CC.YELLOW}No Discord members could be found. Either server is empty or an error has occurred."

        var response = ""
        if (onlineUsers.any { it.onlineStatus == OnlineStatus.ONLINE }) {
            response += "\n${CC.DARK_GREEN}Online:${CC.RESET}"
            for (user in onlineUsers.filter { it.onlineStatus == OnlineStatus.ONLINE }) {
                response += if (user.user.isBot) "\n${CC.DARK_GREEN}- ${user.effectiveName} (Bot)${CC.RESET}"
                else "\n${CC.DARK_GREEN}- ${user.effectiveName}${CC.RESET}"
            }
        }
        if (onlineUsers.any { it.onlineStatus == OnlineStatus.IDLE }) {
            response += "\n${CC.YELLOW}Idle:${CC.RESET}"
            for (user in onlineUsers.filter { it.onlineStatus == OnlineStatus.IDLE }) {
                response += if (user.user.isBot) "\n${CC.YELLOW}- ${user.effectiveName} (Bot)${CC.RESET}"
                else "\n${CC.YELLOW}- ${user.effectiveName}${CC.RESET}"
            }
        }
        if (onlineUsers.any { it.onlineStatus == OnlineStatus.DO_NOT_DISTURB }) {
            response += "\n${CC.RED}Do Not Disturb:${CC.RESET}"
            for (user in onlineUsers.filter { it.onlineStatus == OnlineStatus.DO_NOT_DISTURB }) {
                response += if (user.user.isBot) "\n${CC.RED}- ${user.effectiveName} (Bot)${CC.RESET}"
                else "\n${CC.RED}- ${user.effectiveName}${CC.RESET}"
            }
        }

        response.replaceFirst("\n", "")
        return response.trim()
    }

    /*======================================
      Message Formatting Functions
    ===================================== */

    /**
     * Attempts to convert all instances of "@name" into Discord @tag mentions
     *
     * This should work for "@<Discord server name>", "@<Minecraft username>" (if an alias is linked),
     * and "@<Discord name + #discrminator>"
     *
     * NOTE: If the Discord name contains spaces, that name must be typed in this string without spaces.
     * e.g. a member named "Discord Bridge" must be tagged as "@DiscordBridge"
     *
     * @param message the message to format
     * @return the formatted message
     */
    fun convertAtMentions(message: String): String {
        var newMessage = message

        val discordusers = Connection.listUsers()
        val discordaliases: MutableList<Pair<String, Member>> = mutableListOf()

        for (du in discordusers)
            for ((mcUuid, discordId) in UserAliasConfig.aliases)
                if (discordId == du.user.id) discordaliases.add(Pair(server.getOfflinePlayer(mcUuid).name, du))

        for (match in Regex("""(?:^| )@(\w+)""").findAll(message)) {
            val found: Member? = discordusers.firstOrNull {
                it.user.name.noSpace().toLowerCase() == match.groupValues[1].toLowerCase() ||
                it.user.name + "#" + it.user.discriminator == match.groupValues[1].toLowerCase() ||
                it.effectiveName.noSpace().toLowerCase() == match.groupValues[1].toLowerCase()
            }
            if (found != null) newMessage = newMessage.replaceFirst("@${match.groupValues[1]}", found.asMention)

            val found2: Pair<String, Member>? = discordaliases.firstOrNull {
                it.first.toLowerCase() == match.groupValues[1].toLowerCase()
            }
            if (found2 != null) newMessage = newMessage.replaceFirst("@${match.groupValues[1]}", found2.second.asMention)
        }

        return newMessage
    }

    /**
     * Attempts to de-convert all instances of Discord @tag mentions back into simple "@name" syntax
     *
     * @param message the message to format
     * @return the formatted message
     */
    fun deconvertAtMentions(message: String): String {
        var modifiedMessage = message
        for (match in Regex("""<@!(\d+)>|<@(\d+)>""").findAll(message)) {
            val discordUser = Connection.listUsers().firstOrNull { it.user.id == match.groupValues[1] || it.user.id == match.groupValues[2] }
            if (discordUser != null) modifiedMessage = modifiedMessage.replace(match.value, "@"+discordUser.effectiveName)
        }
        return modifiedMessage
    }

    /**
     * Scans the input string for occurrences of Minecraft names in the alias registry and replaces them with
     * their corresponding Discord aliases
     *
     * @param message the message to format
     * @return the formatted message
     */
    fun translateAliasesToDiscord(message: String): String {
        var modifiedMessage = message
        for ((mcUuid, discordId) in UserAliasConfig.aliases) {
            val nameMC = server.getOfflinePlayer(mcUuid).name
            val discordUser = Connection.listUsers().firstOrNull{it.user.id == discordId }
            val nameDis = if (discordUser != null) discordUser.effectiveName else Connection.JDA.getUserById(discordId).name
            modifiedMessage = modifiedMessage.replace(nameMC, nameDis)
        }
        return modifiedMessage
    }

    /**
     * Scans the input string for occurrences of Discord names in the alias registry and replaces them with
     * their corresponding Minecraft aliases
     *
     * @param message the message to format
     * @return the formatted message
     */
    fun translateAliasesToMinecraft(message: String): String {
        var modifiedMessage = message
        for ((mcUuid, discordId) in UserAliasConfig.aliases) {
            val nameMC = server.getOfflinePlayer(mcUuid).name
            val nameDis = Connection.JDA.getUserById(discordId).name
            modifiedMessage = modifiedMessage.replace(nameDis, nameMC)
            val discordUser = Connection.listUsers().firstOrNull{it.user.id == discordId}
            if (discordUser != null) modifiedMessage = modifiedMessage.replace(discordUser.effectiveName, nameMC)
        }
        return modifiedMessage
    }
}
