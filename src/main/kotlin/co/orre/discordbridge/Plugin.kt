package co.orre.discordbridge

import co.orre.discordbridge.discord.Connection
import co.orre.discordbridge.minecraft.EventListener
import co.orre.discordbridge.minecraft.commands.CommandListener
import co.orre.discordbridge.minecraft.commands.Discord
import co.orre.discordbridge.utils.Rating
import co.orre.discordbridge.utils.Respect
import co.orre.discordbridge.utils.Script
import co.orre.discordbridge.utils.UserAlias
import co.orre.discordbridge.utils.UtilFunctions.noSpace
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.MessageChannel
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.logging.Level
import org.bukkit.ChatColor as CC

class Plugin : JavaPlugin() {

    // Configs
    var users: DataConfigAccessor = DataConfigAccessor(this, dataFolder, "usernames.yml")
    var eightball: DataConfigAccessor = DataConfigAccessor(this, dataFolder, "8ball.yml")
    var insult: DataConfigAccessor = DataConfigAccessor(this, dataFolder, "insult.yml")
    var f: DataConfigAccessor = DataConfigAccessor(this, dataFolder, "f.yml")
    var rate: DataConfigAccessor = DataConfigAccessor(this, dataFolder, "rate.yml")
    var script: DataConfigAccessor = DataConfigAccessor(this, dataFolder, "script.yml")
    var worlds: DataConfigAccessor? = null

    // Temporary storage for alias linking requests
    var requests: MutableList<UserAlias> = mutableListOf()

    // Detects if Multiverse-Core is installed
    val isMultiverseInstalled: Boolean
        get() = server.pluginManager.getPlugin("Multiverse-Core") != null

    override fun onEnable() {
        // Load configs
        updateConfig(description.version)
        if (isMultiverseInstalled) worlds = DataConfigAccessor(this, File("plugins/Multiverse-Core"), "worlds.yml")

        // Connect to Discord
        Connection.plugin = this //TODO: enforce this better
        server.scheduler.runTaskAsynchronously(this, Connection)
        server.pluginManager.registerEvents(EventListener(this), this)

        // Register commands
        //TODO: automate this?
        getCommand("discord").executor = Discord(this) //TODO: move this to Reflection
        getCommand("f").executor = CommandListener(this)
        getCommand("rate").executor = CommandListener(this)
        getCommand("8ball").executor = CommandListener(this)
        getCommand("insult").executor = CommandListener(this)
        getCommand("choose").executor = CommandListener(this)
        getCommand("talk").executor = CommandListener(this)
        getCommand("roll").executor = CommandListener(this)

        ConfigurationSerialization.registerClass(Respect::class.java, "Respect")
        ConfigurationSerialization.registerClass(Rating::class.java, "Rating")
        ConfigurationSerialization.registerClass(Script::class.java, "Script")
    }

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

    // Send a message to Discord
    fun sendToDiscord(message: String, channel: MessageChannel?) {
        logDebug("Sending message to Discord - $message")
        Connection.send(message, channel)
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
        eightball.reloadConfig()
        insult.reloadConfig()
        f.reloadConfig()
        rate.reloadConfig()
        script.reloadConfig()
        if (isMultiverseInstalled) worlds!!.reloadConfig()
        Config.load(this)
        UserAliasConfig.load(this)
        Connection.reconnect()
    }

    // Save default config
    fun updateConfig(version: String) {
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

    // Log only if debug config is true
    fun logDebug(msg: String) {
        if (!Config.DEBUG) return
        logger.info(msg)
    }

    // Get a list of usernames of players who are online
    fun getOnlinePlayers(): List<String> {
        val names: MutableList<String> = mutableListOf()
        val players = server.onlinePlayers.toTypedArray()
        players.mapTo(names) { it.name }
        return names.toList()
    }

    // Open a request to link a Minecraft user with a Discord user
    fun registerUserRequest(player: Player, discordId: String): Boolean {
        val users = Connection.listUsers()
        val found: Member = users.find { it.user.id == discordId } ?: return false

        //val ua: UserAlias = UserAlias(player.name, player.uniqueId.toString(), found.effectiveName, found.user.id)
        val ua: UserAlias = UserAlias(player.uniqueId, found.user.id)
        requests.add(ua)
        val msg = "Minecraft user '${server.getOfflinePlayer(ua.mcUuid).name}' has requested to become associated with your Discord" +
                " account. If this is you, respond '${Connection.JDA.selfUser.asMention} confirm'. If this is not" +
                " you, respond ${Connection.JDA.selfUser.asMention} deny'."
        Connection.send(msg, Connection.JDA.getUserById(ua.discordId).privateChannel)
        return true
    }

    // Return a formatted string listing the Discord IDs of all Discord users in the relay channel
    fun getDiscordIds(): String {
        val users = Connection.listUsers()

        if (users.isEmpty())
            return "${org.bukkit.ChatColor.YELLOW}No Discord members could be found. Either server is empty or an error has occurred."

        var response = "${org.bukkit.ChatColor.YELLOW}Discord users:"
        for (user in users) {
            if (user.user.isBot) response += "\n${org.bukkit.ChatColor.GOLD}- ${user.effectiveName} (Bot), ${user.user.id}${org.bukkit.ChatColor.RESET}"
            else response += "\n${org.bukkit.ChatColor.YELLOW}- ${user.effectiveName}, ${user.user.id}${org.bukkit.ChatColor.RESET}"
        }
        return response
    }

    // Return a formatted string listing all Discord users in the relay channel who are visibly available
    fun getDiscordOnline(): String {
        val onlineUsers = Connection.listOnline()
        if (onlineUsers.isEmpty())
            return "${org.bukkit.ChatColor.YELLOW}No Discord members could be found. Either server is empty or an error has occurred."

        var response = ""
        if (onlineUsers.filter { it.onlineStatus == OnlineStatus.ONLINE }.isNotEmpty()) {
            response += "\n${org.bukkit.ChatColor.DARK_GREEN}Online:${org.bukkit.ChatColor.RESET}"
            for (user in onlineUsers.filter { it.onlineStatus == OnlineStatus.ONLINE }) {
                if (user.user.isBot) response += "\n${org.bukkit.ChatColor.DARK_GREEN}- ${user.effectiveName} (Bot)${org.bukkit.ChatColor.RESET}"
                else response += "\n${org.bukkit.ChatColor.DARK_GREEN}- ${user.effectiveName}${org.bukkit.ChatColor.RESET}"
            }
        }
        if (onlineUsers.filter { it.onlineStatus == OnlineStatus.IDLE }.isNotEmpty()) {
            response += "\n${org.bukkit.ChatColor.YELLOW}Idle:${org.bukkit.ChatColor.RESET}"
            for (user in onlineUsers.filter { it.onlineStatus == OnlineStatus.IDLE }) {
                if (user.user.isBot) response += "\n${org.bukkit.ChatColor.YELLOW}- ${user.effectiveName} (Bot)${org.bukkit.ChatColor.RESET}"
                else response += "\n${org.bukkit.ChatColor.YELLOW}- ${user.effectiveName}${org.bukkit.ChatColor.RESET}"
            }
        }
        if (onlineUsers.filter { it.onlineStatus == OnlineStatus.DO_NOT_DISTURB }.isNotEmpty()) {
            response += "\n${org.bukkit.ChatColor.RED}Do Not Disturb:${org.bukkit.ChatColor.RESET}"
            for (user in onlineUsers.filter { it.onlineStatus == OnlineStatus.DO_NOT_DISTURB }) {
                if (user.user.isBot) response += "\n${org.bukkit.ChatColor.RED}- ${user.effectiveName} (Bot)${org.bukkit.ChatColor.RESET}"
                else response += "\n&${org.bukkit.ChatColor.RED} ${user.effectiveName}${org.bukkit.ChatColor.RESET}"
            }
        }

        response.replaceFirst("\n", "")
        return response
    }

    /*======================================
      Message Formatting Functions
    ===================================== */

    // Converts attempted @mentions to real ones wherever possible
    // Mentionable names MUST NOT contain spaces!
    fun convertAtMentions(message: String): String {
        var newMessage = message

        val discordusers = Connection.listUsers()
        val discordaliases: MutableList<Pair<String, Member>> = mutableListOf()
        discordusers
                .filter { users.data.isSet("discordaliases.${it.user.id}") }
                .mapTo(discordaliases) { Pair(users.data.getString("discordaliases.${it.user.id}.mcusername"), it) }
        for (match in Regex("""(?:^| )@(\w+)""").findAll(message)) {
            val found: Member? = discordusers.firstOrNull {
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

    fun deconvertAtMentions(message: String): String {
        var modifiedMessage = message
        for (match in Regex("""<@!(\d)+>""").findAll(message)) {
            val discordUser = Connection.listUsers().firstOrNull{it.user.id == match.groupValues[1] }
            val nameDis = if (discordUser != null) discordUser.effectiveName else Connection.JDA.getUserById(match.groupValues[1]).name
            modifiedMessage = modifiedMessage.replace(match.value, nameDis)
        }
        return message
    }

    // Scans the string for occurrences of Minecraft names and attempts to translate them
    // to registered Discord names, if they exist
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

    // Scans the string for occurrences of Discord names and attempts to translate them
    // to registered Minecraft names, if they exist
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
