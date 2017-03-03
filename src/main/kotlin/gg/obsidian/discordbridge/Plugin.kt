package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.Utils.noSpace
import gg.obsidian.discordbridge.Utils.UserAlias
import gg.obsidian.discordbridge.discord.Connection
import gg.obsidian.discordbridge.minecraft.commands.*
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.MessageChannel
import org.bukkit.ChatColor as CC
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level
import java.io.File

class Plugin : JavaPlugin() {

    // Connection to Discord
    lateinit var conn: Connection

    // Configs
    val cfg = Configuration(this)
    var users: DataConfigAccessor = DataConfigAccessor(this, dataFolder, "usernames.yml")
    var memory: DataConfigAccessor = DataConfigAccessor(this, dataFolder, "botmemory.yml")
    var insults: DataConfigAccessor = DataConfigAccessor(this, dataFolder, "insults.yml")
    var worlds: DataConfigAccessor? = null

    // Temporary storage for alias linking requests
    var requests: MutableList<UserAlias> = mutableListOf()

    // Detects if Multiverse-Core is installed
    val foundMultiverse: Boolean
        get() = server.pluginManager.getPlugin("Multiverse-Core") != null

    override fun onEnable() {
        // Load configs
        updateConfig(description.version)
        users.saveDefaultConfig()
        memory.saveDefaultConfig()
        insults.saveDefaultConfig()
        if (foundMultiverse) worlds = DataConfigAccessor(this, File("plugins/Multiverse-Core"), "worlds.yml")

        // Connect to Discord
        this.conn = Connection(this)
        server.scheduler.runTaskAsynchronously(this, conn)
        server.pluginManager.registerEvents(EventListener(this), this)

        // Register commands
        getCommand("discord").executor = Discord(this)
        getCommand("f").executor = F(this)
        getCommand("rate").executor = Rate(this)
        getCommand("8ball").executor = EightBall(this)
        getCommand("insult").executor = Insult(this)
    }

    override fun onDisable() {
        if (cfg.ANNOUNCE_SERVER_START_STOP)
            conn.send(cfg.TEMPLATES_DISCORD_SERVER_STOP, conn.getRelayChannel())

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
        conn.send(message, channel)
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
        insults.reloadConfig()
        if (foundMultiverse) worlds!!.reloadConfig()
        cfg.load()
        //conn?.reconnect()
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

    // Get a list of usernames of players who are online
    fun getOnlinePlayers(): List<String> {
        val names: MutableList<String> = mutableListOf()
        val players = server.onlinePlayers.toTypedArray()
        players.mapTo(names) { it.name }
        return names.toList()
    }

    // Open a request to link a Minecraft user with a Discord user
    fun registerUserRequest(player: Player, id: String): Boolean {
        val users = conn.listUsers()
        val found: Member = users.find { it.user.id == id } ?: return false

        val ua: UserAlias = UserAlias(player.name, player.uniqueId.toString(), found.effectiveName, found.user.id)
        requests.add(ua)
        val msg = "Minecraft user '${ua.mcUsername}' has requested to become associated with your Discord" +
                " account. If this is you, respond '${conn.api!!.selfUser.asMention} confirm'. If this is not" +
                " you, respond ${conn.api!!.selfUser.asMention} deny'."
        conn.send(msg, conn.api!!.getUserById(ua.discordId).privateChannel)
        return true
    }

    // Return a formatted string listing the Discord IDs of all Discord users in the relay channel
    fun getDiscordIds(): String {
        val users = conn.listUsers()

        if (users.isEmpty())
            return "${CC.YELLOW}No Discord members could be found. Either server is empty or an error has occurred."

        var response = "${CC.YELLOW}Discord users:"
        for (user in users) {
            if (user.user.isBot) response += "\n${CC.GOLD}- ${user.effectiveName} (Bot), ${user.user.id}${CC.RESET}"
            else response += "\n${CC.YELLOW}- ${user.effectiveName}, ${user.user.id}${CC.RESET}"
        }
        return response
    }

    // Return a formatted string listing all Discord users in the relay channel who are visibly available
    fun getDiscordOnline(): String {
        val onlineUsers = conn.listOnline()
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
                else response += "\n&${CC.RED} ${user.effectiveName}${CC.RESET}"
            }
        }

        response.replaceFirst("\n", "")
        return response
    }

    // Add an alias to the Users data
    fun saveAlias(ua: UserAlias) {
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
    // Mentionable names MUST NOT contain spaces!
    fun convertAtMentions(message: String): String {
        var newMessage = message

        val discordusers = conn.listUsers()
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

    //TODO: De-convert @mentions

    // Scans the string for occurrences of the Minecraft name matching the given UUID and attempts to translate it
    // to a registered Discord name, if it exists
    fun translateAliasToDiscord(message: String, uuid: String?): String {
        var newMessage = message
        val alias = users.data.getString("mcaliases.$uuid.discordusername")
        if (alias != null)
            newMessage = newMessage.replace(users.data.getString("mcaliases.$uuid.mcusername"), alias)
        return newMessage
    }

    // Scans the string for occurrences of the Minecraft name matching the given UUID and attempts to translate it
    // to a registered Discord name, if it exists
    fun translateAliasToMinecraft(message: String, discordId: String?): String {
        var newMessage = message
        val alias = users.data.getString("discordaliases.$discordId.mcusername")
        if (alias != null)
            newMessage = newMessage.replace(users.data.getString("discordaliases.$discordId.mcusername"), alias)
        return newMessage
    }

    fun toMinecraftChatMessage(message: String, alias: String): String {
        return formatMessage(cfg.TEMPLATES_MINECRAFT_CHAT_MESSAGE, msg=message, u=alias)
    }

    fun toDiscordChatMessage(message: String, username: String, displayName: String, worldName: String): String {
        return formatMessage(cfg.TEMPLATES_DISCORD_CHAT_MESSAGE, msg=message, u=username, d=displayName, w=worldName)
    }

    fun toDiscordPlayerJoin(username: String, displayName: String): String {
        return formatMessage(cfg.TEMPLATES_DISCORD_PLAYER_JOIN, u=username, d=displayName)
    }

    fun toDiscordPlayerLeave(username: String, displayName: String): String {
        return formatMessage(cfg.TEMPLATES_DISCORD_PLAYER_LEAVE, u=username, d=displayName)
    }

    fun toDiscordPlayerDeath(deathMessage: String, username: String, displayName: String): String {
        return formatMessage(cfg.TEMPLATES_DISCORD_CHAT_MESSAGE, r=deathMessage, u=username, d=displayName)
    }

    private fun formatMessage(template: String, msg: String = "N/A", u: String = "N/A", d: String = "N/A",
                              w: String = "N/A", r: String = "N/A"): String {
        var out = CC.translateAlternateColorCodes('&', template)
        out = out.replace("%u", u).replace("%m", msg).replace("%d", d).replace("%w", w).replace("%r", r)
        return out
    }
}
