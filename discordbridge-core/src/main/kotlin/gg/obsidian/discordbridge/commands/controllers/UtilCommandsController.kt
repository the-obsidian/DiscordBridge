package gg.obsidian.discordbridge.commands.controllers

import gg.obsidian.discordbridge.DiscordBridge
import gg.obsidian.discordbridge.UserAliasConfig
import gg.obsidian.discordbridge.commands.*
import gg.obsidian.discordbridge.commands.annotations.*
import gg.obsidian.discordbridge.discord.Connection
import gg.obsidian.discordbridge.util.ChatColor as CC
import gg.obsidian.discordbridge.util.UtilFunctions.stripColor
import gg.obsidian.discordbridge.wrappers.IPlayer
import net.dv8tion.jda.core.entities.ChannelType
import java.util.*

/**
 * Controller for utility commands that configure the way the bot works
 *
 * @param db a reference to the base Plugin object
 */
class UtilCommandsController(val db: DiscordBridge) : IBotController {

    /**
     * @return a short description of this IBotController's methods as seen in the Help command
     */
    override fun getDescription(): String = ":wrench:  **UTIL** - Utility commands"

    /**
     * Confirms an open request to link aliases with a Minecraft account
     *
     * @param event the incoming event object
     * @return a status message if the event is type DiscordMessageWrapper and is sent via DM, null otherwise
     */
    @BotCommand(usage="", description="Confirm an alias link request", relayTriggerMessage = false)
    @DiscordExclusiveCommand
    @PrivateResponse
    private fun confirm(event: IEventWrapper): String? {
        if (event !is DiscordMessageWrapper) return null
        if (!event.originalMessage.isFromType(ChannelType.PRIVATE)) {
            event.channel.sendMessage("${event.senderAsMention} | That command can only be sent to me via DM.").queue()
            return null
        }
        db.logDebug("user ${event.senderName} confirms an alias request")

        val ua = db.requests.find { it.discordId == event.senderId } ?: return "You have no alias requests pending."

        UserAliasConfig.add(db, ua)
        db.requests.remove(ua)
        return "Successfully linked aliases!"
    }

    /**
     * Denies an open request to link aliases with a Minecraft account
     *
     * @param event the incoming event object
     * @return a status message if the event is type DiscordMessageWrapper and is sent via DM, null otherwise
     */
    @BotCommand(usage="", description="Deny an alias link request", relayTriggerMessage = false)
    @DiscordExclusiveCommand
    @PrivateResponse
    private fun deny(event: IEventWrapper): String? {
        if (event !is DiscordMessageWrapper) return null
        if (!event.originalMessage.isFromType(ChannelType.PRIVATE)) {
            event.channel.sendMessage("${event.senderAsMention} | That command can only be sent to me via DM.").queue()
            return null
        }
        db.logDebug("user ${event.senderName} denies an alias request")

        val ua = db.requests.find { it.discordId == event.senderId } ?: return "You have no alias requests pending."

        db.requests.remove(ua)
        return "The alias link request has been cancelled."
    }

    /**
     * A supercommand for utility functions available within Minecraft
     *
     * reload - Refreshes the JDA connection and reloads configs
     *
     * linkalias - sends a request to a specified Discord user to link aliases for username translation
     *
     * listmembers - lists all the members in the Discord relay channel. Can specify "all" to get all members, or
     * "online" to list only online members and their availability statuses
     *
     * unlinkalias - silently breaks an alias link with a Discord user if one exists
     *
     * @param event the incoming event object
     * @param argString a space-delimited string of subcommands to execute
     * @return this command always returns null
     */
    @BotCommand(usage="<reload|linkalias|listmembers|unlinkalias> [args...]", description = "For in-game functions", relayTriggerMessage = false, squishExcessArgs = true)
    @MinecraftExclusiveCommand
    @PrivateResponse
    private fun discord(event: IEventWrapper, argString: String): String? {
        if (event !is MinecraftCommandWrapper) return null
        if (argString.isBlank()) {
            event.sender.sendMessage("${CC.YELLOW}Usage: /discord <reload|listmembers|linkalias>")
            return null
        }

        val args = argString.split(" ")
        val subCommand = args[0]

        when (subCommand.toLowerCase()) {
            "reload" -> {
                if (event.sender is IPlayer && !event.sender.hasPermission("discordbridge.discord.reload")) {
                    event.sender.sendMessage("${CC.ITALIC}${CC.GRAY}${db.getConfig().getString("username", "DiscordBridge").stripColor()} whispers to you: " +
                            "Sorry, you don't have permission to use that command.")
                } else {
                    event.sender.sendMessage("${CC.YELLOW}Reloading Discord Bridge...")
                    db.reload(Runnable { event.sender.sendMessage("${CC.DARK_GREEN}Discord Bridge has reloaded!") })
                }
            }
            "linkalias" -> {
                if (event.sender !is IPlayer) return null

                if (!event.sender.hasPermission("discordbridge.discord.linkalias")) {
                    event.sender.sendMessage("${CC.ITALIC}${CC.GRAY}${db.getConfig().getString("username", "DiscordBridge").stripColor()} whispers to you: " +
                            "Sorry, you don't have permission to use that command.")
                }

                if (args.size < 2) {
                    event.sender.sendMessage("${CC.YELLOW}Usage: /discord linkalias <discriminator> [-u]")
                    return null
                }

                val ua = UserAliasConfig.aliases.firstOrNull{UUID.fromString(it.mcUuid) == event.sender.getUUID()}
                if (ua != null) {
                    event.sender.sendMessage("${CC.YELLOW}You already have an alias linked with Discord user " +
                            "${Connection.JDA.getUserById(ua.discordId)}. " +
                            "If you want to unlink this account, use '/discord unlinkalias'.")
                    return null
                }

                val pendingRequest = db.requests.firstOrNull{ UUID.fromString(it.mcUuid) == event.sender.getUUID()}
                if (pendingRequest != null) {
                    if (args.size < 3 || args[3] != "-u") {
                        event.sender.sendMessage("${CC.YELLOW}You already have an alias link request pending with " +
                                "Discord user '${Connection.JDA.getUserById(pendingRequest.discordId)}'. " +
                                "If you want to override this request, repeat this command with the '-u' flag.")
                        return null
                    } else if (args[3] == "-u")
                        db.requests.remove(pendingRequest)
                }

                val foundMember = db.registerUserRequest(event.sender, args[1])
                if (foundMember == null)
                    event.sender.sendMessage("${CC.YELLOW}Could not find Discord user with that discriminator. " +
                            "Try '/discord listmembers all' to see a list of valid users.")
                else event.sender.sendMessage("${CC.YELLOW}An alias link request has been sent to Discord user '${foundMember.user.name}'")
            }
            "listmembers" -> {
                if (args.size < 2) {
                    event.sender.sendMessage("${CC.YELLOW}Usage: /discord listmembers <all|online>")
                    return null
                }

                if (event.sender is IPlayer && !event.sender.hasPermission("discordbridge.discord.listmembers")) {
                    event.sender.sendMessage("${CC.ITALIC}${CC.GRAY}${db.getConfig().getString("username", "DiscordBridge").stripColor()} whispers to you: " +
                            "Sorry, you don't have permission to use that command.")
                }

                when (args[1].toLowerCase()) {
                    "all" -> event.sender.sendMessage(db.getDiscordMembersAll())
                    "online" -> event.sender.sendMessage(db.getDiscordMembersOnline())
                    else -> event.sender.sendMessage("${CC.YELLOW}Usage: /discord listmembers <all/online>")
                }
            }
            "unlinkalias" -> {
                if (event.sender !is IPlayer) return null

                val ua = UserAliasConfig.aliases.firstOrNull{UUID.fromString(it.mcUuid) == event.sender.getUUID()}
                if (ua == null) {
                    event.sender.sendMessage("${CC.YELLOW}You do not have an alias to unlink.")
                    return null
                }
                UserAliasConfig.remove(db, ua)
                event.sender.sendMessage("${CC.YELLOW}Alias link with Discord user ${Connection.JDA.getUserById(ua.discordId)} " +
                        "succeessfully removed.")
            }
        }
        return null
    }

    /**
     * Displays a pretty list of available commands to the invoker
     *
     * @param event the incoming event object
     * @param commands a map of all commands that can be called by the invoker in the given medium indexed by name
     * @param instances a map of IBotController instances accessed by their Java classes
     */
    @BotCommand(usage="", description="You just used it", relayTriggerMessage = false)
    @ChatExclusiveCommand
    @PrivateResponse
    private fun help(event: IEventWrapper, commands: MutableMap<String, BotControllerManager.Command>,
                     instances: Map<Class<out IBotController>, IBotController>) {
        db.logger.info("Entered 'help'")
        db.logDebug("user ${event.senderName} requested help")
        when (event) {
            is MinecraftChatEventWrapper -> {
                val player = event.player
                player.sendMessage("${CC.BOLD}${CC.AQUA}DiscordBridge${CC.RESET} - Bridge your Minecraft and Discord chats\n---")
                var out: String
                for (bc: IBotController in instances.values) {
                    out = bc.getDescription() + "\n```"
                    commands.values.sortedBy { (name) -> name }
                            .filter { it.controllerClass == bc.javaClass }
                            .forEach { out += "\n${it.name} ${it.usage}\n  ${it.description}\n" }
                    out += "```"
                    player.sendMessage(out)
                }
            }
            is DiscordMessageWrapper -> {
                db.logger.info("DiscordMessageWrapper detected!")
                event.originalMessage.author.openPrivateChannel().queue({p ->
                    db.logger.info("RestAction returned: $p")
                    p.sendMessage("**DiscordBridge** - Bridge your Minecraft and Discord chats\n**---**").queue()
                    var out: String
                    for (bc: IBotController in instances.values) {
                        out = bc.getDescription() + "\n```"
                        commands.values.sortedBy { (name) -> name }
                                .filter { it.controllerClass == bc.javaClass }
                                .forEach { out += "\n${it.name} ${it.usage}\n  ${it.description}\n" }
                        out += "```"
                        p.sendMessage(out).queue()
                    }
                })
                db.logger.info("Continuing non-blocking thread")
            }
        }
    }

    /**
     * Displays a pretty list of all currently logged-in Minecraft players
     *
     * @param event the incoming event object
     */
    @DiscordExclusiveCommand
    @BotCommand(usage="", description = "List all Minecraft players currently online on the server",
            relayTriggerMessage = false)
    private fun serverList(event: IEventWrapper) {
        if (event !is DiscordMessageWrapper) return
        db.logDebug("user ${event.originalMessage.author.name} has requested a listing of online players")

        val players = db.getOnlinePlayers()
        if (players.isEmpty()) {
            db.sendToDiscord("Nobody is currently online.", event.channel)
            return
        }

        val response = players.joinToString("\n", "The following players are currently online:\n```\n", "\n```")
        db.sendToDiscord(response, event.channel)
    }
}