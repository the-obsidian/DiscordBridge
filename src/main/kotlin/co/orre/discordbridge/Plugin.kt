package co.orre.discordbridge

import co.orre.discordbridge.discord.Connection
import co.orre.discordbridge.minecraft.CommandListener
import co.orre.discordbridge.minecraft.EventListener
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

    // Reloads everything
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
    fun registerUserRequest(player: Player, discriminator: String): Member? {
        val users = Connection.listUsers()
        val found: Member = users.find { it.user.name + "#" + it.user.discriminator == discriminator } ?: return null

        val ua: UserAlias = UserAlias(player.uniqueId, found.user.id)
        requests.add(ua)
        val msg = "Minecraft user '${server.getOfflinePlayer(ua.mcUuid).name}' has requested to become associated with your Discord" +
                " account. If this is you, respond '${Connection.JDA.selfUser.asMention} confirm'. If this is not" +
                " you, respond ${Connection.JDA.selfUser.asMention} deny'."
        val member = Connection.JDA.getUserById(ua.discordId)
        member.openPrivateChannel().queue({p -> p.sendMessage(msg).queue()})
        return found
    }

    // Return a formatted string listing the Discord IDs of all Discord users in the relay channel
    fun getDiscordMembersAll(): String {
        val users = Connection.listUsers()

        if (users.isEmpty())
            return "${CC.YELLOW}No Discord members could be found. Either server is empty or an error has occurred."

        var response = "${CC.YELLOW}Discord users:"
        for (user in users) {
            if (user.user.isBot) response += "\n${CC.GOLD}- ${user.effectiveName} (Bot) | ${user.user.name}#${user.user.discriminator}${CC.RESET}"
            else response += "\n${CC.YELLOW}- ${user.effectiveName} | ${user.user.name}#${user.user.discriminator}${CC.RESET}"
        }
        return response.trim()
    }

    // Return a formatted string listing all Discord users in the relay channel who are visibly available
    fun getDiscordMembersOnline(): String {
        val onlineUsers = Connection.listOnline()
        if (onlineUsers.isEmpty())
            return "${CC.YELLOW}No Discord members could be found. Either server is empty or an error has occurred."

        var response = ""
        if (onlineUsers.filter { it.onlineStatus == OnlineStatus.ONLINE }.isNotEmpty()) {
            response += "\n${CC.DARK_GREEN}Online:${CC.RESET}"
            for (user in onlineUsers.filter { it.onlineStatus == OnlineStatus.ONLINE }) {
                if (user.user.isBot) response += "\n${CC.DARK_GREEN}- ${user.effectiveName} (Bot)${CC.RESET}"
                else response += "\n${CC.DARK_GREEN}- ${user.effectiveName}${CC.RESET}"
            }
        }
        if (onlineUsers.filter { it.onlineStatus == OnlineStatus.IDLE }.isNotEmpty()) {
            response += "\n${CC.YELLOW}Idle:${CC.RESET}"
            for (user in onlineUsers.filter { it.onlineStatus == OnlineStatus.IDLE }) {
                if (user.user.isBot) response += "\n${CC.YELLOW}- ${user.effectiveName} (Bot)${CC.RESET}"
                else response += "\n${CC.YELLOW}- ${user.effectiveName}${CC.RESET}"
            }
        }
        if (onlineUsers.filter { it.onlineStatus == OnlineStatus.DO_NOT_DISTURB }.isNotEmpty()) {
            response += "\n${CC.RED}Do Not Disturb:${CC.RESET}"
            for (user in onlineUsers.filter { it.onlineStatus == OnlineStatus.DO_NOT_DISTURB }) {
                if (user.user.isBot) response += "\n${CC.RED}- ${user.effectiveName} (Bot)${CC.RESET}"
                else response += "\n${CC.RED}- ${user.effectiveName}${CC.RESET}"
            }
        }

        response.replaceFirst("\n", "")
        return response.trim()
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

    fun deconvertAtMentions(message: String): String {
        var modifiedMessage = message
        for (match in Regex("""<@!(\d+)>|<@(\d+)>""").findAll(message)) {
            val discordUser = Connection.listUsers().firstOrNull { it.user.id == match.groupValues[1] || it.user.id == match.groupValues[2] }
            if (discordUser != null) modifiedMessage = modifiedMessage.replace(match.value, "@"+discordUser.effectiveName)
        }
        return modifiedMessage
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
