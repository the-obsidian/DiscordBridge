package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.commands.MinecraftChatEventWrapper
import gg.obsidian.discordbridge.commands.MinecraftCommandWrapper
import gg.obsidian.discordbridge.commands.controllers.BotControllerManager
import gg.obsidian.discordbridge.commands.controllers.FunCommandsController
import gg.obsidian.discordbridge.commands.controllers.UtilCommandsController
import gg.obsidian.discordbridge.discord.Connection
import gg.obsidian.discordbridge.util.Cfg
import gg.obsidian.discordbridge.util.MarkdownToMinecraftSeralizer
import gg.obsidian.discordbridge.util.UserAlias
import gg.obsidian.discordbridge.util.ChatColor as CC
import gg.obsidian.discordbridge.util.UtilFunctions.noSpace
import gg.obsidian.discordbridge.util.UtilFunctions.toDiscordChatMessage
import gg.obsidian.discordbridge.util.UtilFunctions.toDiscordPlayerJoin
import gg.obsidian.discordbridge.util.UtilFunctions.toDiscordPlayerLeave
import gg.obsidian.discordbridge.util.UtilFunctions.toDiscordPlayerDeath
import gg.obsidian.discordbridge.wrappers.*
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.MessageChannel
import org.pegdown.PegDownProcessor
import java.io.File
import java.io.IOException
import java.io.FileOutputStream
import java.util.*

class DiscordBridge(private val server: IServer, dataFolder: File) {

    private val pegDownProc = PegDownProcessor()
    private val minecraftChatControllerManager = BotControllerManager(this)
    private val discordChatControllerManager = BotControllerManager(this)
    private val minecraftCommandControllerManager = BotControllerManager(this)
    private val cfgNodes: MutableMap<Cfg, ConfigurationNode> = mutableMapOf()

    // Temporary storage for alias linking requests
    var requests: MutableList<UserAlias> = mutableListOf()

    val logger: ILogger = server.getLogger()

    init {
        Connection.db  = this //TODO: enforce this better

        if (!dataFolder.exists()) dataFolder.mkdirs()

        for (c in Cfg.values()) {
            val filename = "${c.filename}.yml"
            val file = File(dataFolder, filename)
            if (!createDefaultFileFromResource("/$filename", file))
                logger.severe("Could not create default file for $filename")
            cfgNodes.put(c, ConfigurationNode(file))
        }

        minecraftChatControllerManager.registerController(FunCommandsController(this), chatExclusive = true)
        minecraftChatControllerManager.registerController(UtilCommandsController(this), chatExclusive = true)
        minecraftCommandControllerManager.registerController(FunCommandsController(this), minecraftExclusive = true)
        minecraftCommandControllerManager.registerController(UtilCommandsController(this), minecraftExclusive = true)
        discordChatControllerManager.registerController(FunCommandsController(this), discordExclusive = true, chatExclusive = true)
        discordChatControllerManager.registerController(UtilCommandsController(this), discordExclusive = true, chatExclusive = true)
    }

    fun postInit() {
        for (cfg in cfgNodes.values) cfg.load()
        UserAliasConfig.load(this)
        server.getScheduler().runAsyncTask(Connection)
    }

    fun logDebug(msg: String) {
        if (!getConfig(Cfg.CONFIG).getBoolean("debug", false)) return
        logger.info("[DiscordBridge] $msg")
    }

    fun getServer(): IServer = server
    fun getPegDownProcessor(): PegDownProcessor = pegDownProc
    fun getConfig(type: Cfg) = cfgNodes[type]!!

    // Borrowed code from dynmap-core
    /* Uses resource to create default file, if file does not yet exist */
    private fun createDefaultFileFromResource(resourcename: String, deffile: File): Boolean {
        if (deffile.canRead()) return true
        logger.info(deffile.path + " not found - creating default")
        val inputStream = javaClass.getResourceAsStream(resourcename)
        if (inputStream == null) {
            logger.severe("Unable to find default resource - " + resourcename)
            return false
        } else {
            var fos: FileOutputStream? = null
            try {
                fos = FileOutputStream(deffile)
                while (inputStream.copyTo(fos, 512) > 0) { }
            } catch (iox: IOException) {
                logger.severe("ERROR creating default for " + deffile.path)
                return false
            } finally {
                if (fos != null)
                    try { fos.close() }
                    catch (iox: IOException) { }

                try { inputStream.close() }
                catch (iox: IOException) { }

            }
            return true
        }
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


    fun reload(callback: Runnable) {
        for (cfg in cfgNodes.values) cfg.load()
        //if (isMultiverseInstalled) worlds!!.reloadConfig()
        UserAliasConfig.load(this)
        Connection.reconnect(callback)
    }

    /**
     * @return a list of names of all players currently on the Minecraft server
     */
    fun getOnlinePlayers(): List<String> {
        val names: MutableList<String> = mutableListOf()
        val players = server.getOnlinePlayers().toTypedArray()
        players.mapTo(names) { it.getName() }
        return names.toList()
    }

    /**
     * Opens an alias link request and sends it to the target Discord user
     *
     * @param player the Minecraft player that initiated the request
     * @param discriminator the Discord username+discriminator of the target Discord user
     * @return a Discord Member object, or null if no matching member was found
     */
    fun registerUserRequest(player: IPlayer, discriminator: String): Member? {
        val users = Connection.listUsers()
        val found: Member = users.find { it.user.name + "#" + it.user.discriminator == discriminator } ?: return null

        val ua = UserAlias(player.getUUID().toString(), found.user.id)
        requests.add(ua)
        val msg = "Minecraft user '${player.getName()}' has requested to become associated with your Discord" +
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
                if (discordId == du.user.id) {
                    val player = server.getPlayer(UUID.fromString(mcUuid))
                    if (player != null) discordaliases.add(Pair(player.getName(), du))
                }

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
            val player = server.getPlayer(UUID.fromString(mcUuid))
            if (player != null) {
                val nameMC = player.getName()
                val discordUser = Connection.listUsers().firstOrNull { it.user.id == discordId }
                val nameDis = if (discordUser != null) discordUser.effectiveName else Connection.JDA.getUserById(discordId).name
                modifiedMessage = modifiedMessage.replace(nameMC, nameDis)
            }
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
            val player = server.getPlayer(UUID.fromString(mcUuid))
            if (player != null) {
                val nameMC = player.getName()
                val nameDis = Connection.JDA.getUserById(discordId).name
                modifiedMessage = modifiedMessage.replace(nameDis, nameMC)
                val discordUser = Connection.listUsers().firstOrNull { it.user.id == discordId }
                if (discordUser != null) modifiedMessage = modifiedMessage.replace(discordUser.effectiveName, nameMC)
            }
        }
        return modifiedMessage
    }

    fun handleServerStart() {
        if (getConfig(Cfg.CONFIG).getBoolean("announce-server-start-stop", true))
            sendToDiscord(getConfig(Cfg.CONFIG).getString("templates.discord.server-start", "Server started!"), Connection.getRelayChannel())
    }

    fun handleServerStop() {
        if (getConfig(Cfg.CONFIG).getBoolean("announce-server-start-stop", true))
            sendToDiscord(getConfig(Cfg.CONFIG).getString("templates.discord.server-stop", "Shutting down..."), Connection.getRelayChannel())
    }

    fun handlePlayerChat(player: IPlayer, message: String, isCancelled: Boolean): String {
        // TODO: the order of these if statements may produce undesired behavior
        logDebug("Received a chat event from ${player.getName()}: $message")
        if (!getConfig(Cfg.CONFIG).getBoolean("player-messages.chat", true)) return message
        if (isCancelled && !getConfig(Cfg.CONFIG).getBoolean("relay-cancelled-messages", true)) return message
        if (player.isVanished() && !getConfig(Cfg.CONFIG).getBoolean("if-vanished.player-chat", false)) return message

        // Emoticons!
        var newMessage = message.replace(":lenny:", "( \u0361\u00B0 \u035C\u0296 \u0361\u00B0)")
                .replace(":tableflip:", "(\u256F\u00B0\u25A1\u00B0\uFF09\u256F\uFE35 \u253B\u2501\u253B")
                .replace(":unflip:", "\u252C\u2500\u2500\u252C \u30CE( \u309C-\u309C\u30CE)")
                .replace(":shrug:", "\u00AF\\_(\u30C4)_/\u00AF")
                .replace(":donger:", "\u30FD\u0F3C\u0E88\u0644\u035C\u0E88\u0F3D\uFF89")
                .replace(":disapproval:", "\u0CA0_\u0CA0")
                .replace(":kawaii:", "(\uFF89\u25D5\u30EE\u25D5)\uFF89*:\uFF65\uFF9F\u2727")
                .replace(":amendo:", "\u0F3C \u3064 \u25D5_\u25D5 \u0F3D\u3064")
                .replace(":yuno:", "\u10DA(\u0CA0\u76CA\u0CA0\u10DA)")
                .replace(":fingerguns:", "(\u261E\uFF9F\u30EE\uFF9F)\u261E")
                .replace(":fingergunsr:", "(\u261E\uFF9F\u30EE\uFF9F)\u261E")
                .replace(":fingergunsl:", "\u261C(\uFF9F\u30EE\uFF9F\u261C)")
                .replace(":fight:", "(\u0E07 \u2022\u0300_\u2022\u0301)\u0E07")
                .replace(":happygary:", "\u1555(\u141B)\u1557")
                .replace(":denko:", "(\u00B4\uFF65\u03C9\uFF65`)")
                .replace(":masteryourdonger:", "(\u0E07 \u0360\u00B0 \u0644\u035C \u00B0)\u0E07")

        //val wrapper = MinecraftChatEventWrapper(AsyncPlayerChatEvent(true, event.player, event.message, event.recipients))
        server.getScheduler().runAsyncTask(Runnable { minecraftChatControllerManager.dispatchMessage(MinecraftChatEventWrapper(player, newMessage)) })

        newMessage = MarkdownToMinecraftSeralizer().toMinecraft(pegDownProc.parseMarkdown(newMessage.toCharArray()))
        return newMessage
    }

    fun handlePlayerJoin(player: IPlayer) {
        val username = player.getName()
        val worldname = player.getWorld().getName()
        logDebug("Received a join event for $username")
        if (!getConfig(Cfg.CONFIG).getBoolean("messages.player-join", true)) return
        if (player.isVanished() && !getConfig(Cfg.CONFIG).getBoolean("if-vanished.player-join", false)) return

        // Get world alias if Multiverse is installed
//        if (plugin.isMultiverseInstalled) {
//            val worldProperties = plugin.worlds!!.data.get("worlds.$worldname")
//            val cls = Class.forName("com.onarandombox.MultiverseCore.WorldProperties")
//            val meth: Method = cls.getMethod("getAlias")
//            val alias = meth.invoke(worldProperties)
//            if (alias is String) worldname = alias
//        }

        var formattedMessage = player.toDiscordPlayerJoin(this, worldname)
        formattedMessage = translateAliasesToDiscord(formattedMessage)
        sendToDiscord(formattedMessage, Connection.getRelayChannel())
    }

    fun handlePlayerQuit(player: IPlayer) {
        val username = player.getName()
        val worldname = player.getWorld().getName()
        logDebug("Received a leave event for $username")
        if (!getConfig(Cfg.CONFIG).getBoolean("messages.player-leave", true)) return
        if (player.isVanished() && !getConfig(Cfg.CONFIG).getBoolean("if-vanished.player-leave", false)) return

        // Get world alias if Multiverse is installed
//        if (plugin.isMultiverseInstalled) {
//            val worldProperties = plugin.worlds!!.data.get("worlds.$worldname")
//            val cls = Class.forName("com.onarandombox.MultiverseCore.WorldProperties")
//            val meth: Method = cls.getMethod("getAlias")
//            val alias = meth.invoke(worldProperties)
//            if (alias is String) worldname = alias
//        }

        var formattedMessage = player.toDiscordPlayerLeave(this, worldname)
        formattedMessage = translateAliasesToDiscord(formattedMessage)
        sendToDiscord(formattedMessage, Connection.getRelayChannel())
    }

    fun handlePlayerDeath(player: IPlayer, deathMessage: String) {
        val username = player.getName()
        val worldname = player.getWorld().getName()

        if (!getConfig(Cfg.CONFIG).getBoolean("messages.player-death", false)) return
        if (player.isVanished() && !getConfig(Cfg.CONFIG).getBoolean("if-vanished.player-death", false)) return

        // Get world alias if Multiverse is installed
//        if (plugin.isMultiverseInstalled) {
//            val worldProperties = plugin.worlds!!.data.get("worlds.$worldname")
//            val cls = Class.forName("com.onarandombox.MultiverseCore.WorldProperties")
//            val meth: Method = cls.getMethod("getAlias")
//            val alias = meth.invoke(worldProperties)
//            if (alias is String) worldname = alias
//        }

        var formattedMessage = deathMessage.toDiscordPlayerDeath(this, username, worldname)
        formattedMessage = translateAliasesToDiscord(formattedMessage)
        sendToDiscord(formattedMessage, Connection.getRelayChannel())
    }

    fun handleDynmapChat(name: String, message: String) {
        sendToDiscord(translateAliasesToDiscord(message.toDiscordChatMessage(this, name, "Dynmap")), Connection.getRelayChannel())
    }

    fun handleCommand(sender: ICommandSender, commandName: String, args: Array<out String>): Boolean {
        return minecraftCommandControllerManager.dispatchMessage(MinecraftCommandWrapper(sender, commandName, args))
    }

    fun getServerCommands(): List<BotControllerManager.Command> {
        return minecraftCommandControllerManager.getCommands().values.toList()
    }

}