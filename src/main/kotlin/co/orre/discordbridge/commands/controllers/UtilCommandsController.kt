package co.orre.discordbridge.commands.controllers

import co.orre.discordbridge.Config
import co.orre.discordbridge.Plugin
import co.orre.discordbridge.UserAliasConfig
import co.orre.discordbridge.commands.*
import co.orre.discordbridge.commands.annotations.*
import co.orre.discordbridge.discord.Connection
import co.orre.discordbridge.utils.UtilFunctions.stripColor
import net.dv8tion.jda.core.entities.ChannelType
import org.bukkit.entity.Player
import org.bukkit.ChatColor as CC

class UtilCommandsController(val plugin: Plugin) : IBotController {

    override fun getDescription(): String = ":wrench:  **UTIL** - Utility commands"

    // CONFIRM - Confirm an alias request
    @BotCommand(usage="", description="Confirm an alias link request", relayTriggerMessage = false)
    @DiscordExclusiveCommand
    @PrivateResponse
    private fun confirm(event: IEventWrapper): String? {
        if (event !is MessageWrapper) return null
        if (!event.originalMessage.isFromType(ChannelType.PRIVATE)) {
            event.channel.sendMessage("${event.senderAsMention} | That command can only be sent to me via DM.").queue()
            return null
        }
        plugin.logDebug("user ${event.senderName} confirms an alias request")

        val ua = plugin.requests.find { it.discordId == event.senderId } ?: return "You have no alias requests pending."

        UserAliasConfig.add(plugin, ua)
        plugin.requests.remove(ua)
        return "Successfully linked aliases!"
    }

    // DENY - Deny an alias request
    @BotCommand(usage="", description="Deny an alias link request", relayTriggerMessage = false)
    @DiscordExclusiveCommand
    @PrivateResponse
    private fun deny(event: IEventWrapper): String? {
        if (event !is MessageWrapper) return null
        if (!event.originalMessage.isFromType(ChannelType.PRIVATE)) {
            event.channel.sendMessage("${event.senderAsMention} | That command can only be sent to me via DM.").queue()
            return null
        }
        plugin.logDebug("user ${event.senderName} denies an alias request")

        val ua = plugin.requests.find { it.discordId == event.senderId } ?: return "You have no alias requests pending."

        plugin.requests.remove(ua)
        return "The alias link request has been cancelled."
    }

    @BotCommand(usage="<reload|get|alias> [args...]", description = "For in-game functions", relayTriggerMessage = false)
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
                if (event.sender is Player && !event.sender.hasPermission("discordbridge.discord.reload")) {
                    event.sender.sendMessage("${CC.ITALIC}${CC.GRAY}${Config.BOT_MC_USERNAME.stripColor()} whispers to you: " +
                            "Sorry, you don't have permission to use that command.")
                } else {
                    event.sender.sendMessage("${CC.YELLOW}Reloading Discord Bridge...")
                    plugin.reload(Runnable { event.sender.sendMessage("${CC.DARK_GREEN}Discord Bridge has reloaded!") })
                }
            }
            "linkalias" -> {
                if (event.sender !is Player) return null

                if (!event.sender.hasPermission("discordbridge.discord.linkalias")) {
                    event.sender.sendMessage("${CC.ITALIC}${CC.GRAY}${Config.BOT_MC_USERNAME.stripColor()} whispers to you: " +
                            "Sorry, you don't have permission to use that command.")
                }

                if (args.size < 2) {
                    event.sender.sendMessage("${CC.YELLOW}Usage: /discord linkalias <discriminator> [-u]")
                    return null
                }

                val ua = UserAliasConfig.aliases.firstOrNull{it.mcUuid == event.sender.uniqueId}
                if (ua != null) {
                    event.sender.sendMessage("${CC.YELLOW}You already have an alias linked with Discord user " +
                            "${Connection.JDA.getUserById(ua.discordId)}. " +
                            "If you want to unlink this account, use '/discord unlinkalias'.")
                    return null
                }

                val pendingRequest = plugin.requests.firstOrNull{it.mcUuid == event.sender.uniqueId}
                if (pendingRequest != null) {
                    if (args.size < 3 || args[3] != "-u") {
                        event.sender.sendMessage("${CC.YELLOW}You already have an alias link request pending with " +
                                "Discord user '${Connection.JDA.getUserById(pendingRequest.discordId)}'. " +
                                "If you want to override this request, repeat this command with the '-u' flag.")
                        return null
                    } else if (args[3] == "-u")
                        plugin.requests.remove(pendingRequest)
                }

                val foundMember = plugin.registerUserRequest(event.sender, args[1])
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

                if (event.sender is Player && !event.sender.hasPermission("discordbridge.discord.listmembers")) {
                    event.sender.sendMessage("${CC.ITALIC}${CC.GRAY}${Config.BOT_MC_USERNAME.stripColor()} whispers to you: " +
                            "Sorry, you don't have permission to use that command.")
                }

                when (args[1].toLowerCase()) {
                    "all" -> event.sender.sendMessage(plugin.getDiscordMembersAll())
                    "online" -> event.sender.sendMessage(plugin.getDiscordMembersOnline())
                    else -> event.sender.sendMessage("${CC.YELLOW}Usage: /discord listmembers <all/online>")
                }
            }
            "unlinkalias" -> {
                if (event.sender !is Player) return null

                val ua = UserAliasConfig.aliases.firstOrNull{it.mcUuid == event.sender.uniqueId}
                if (ua == null) {
                    event.sender.sendMessage("${CC.YELLOW}You do not have an alias to unlink.")
                    return null
                }
                UserAliasConfig.remove(plugin, ua)
                event.sender.sendMessage("${CC.YELLOW}Alias link with Discord user ${Connection.JDA.getUserById(ua.discordId)} " +
                        "succeessfully removed.")
            }
        }
        return null
    }

    @BotCommand(usage="", description="You just used it", relayTriggerMessage = false)
    @ChatExclusiveCommand
    @PrivateResponse
    private fun help(event: IEventWrapper, commands: MutableMap<String, BotControllerManager.Command>,
                     instances: Map<Class<out IBotController>, IBotController>) {
        plugin.logger.info("Entered 'help'")
        plugin.logDebug("user ${event.senderName} requested help")
        when (event) {
            is AsyncPlayerChatEventWrapper -> {
                val player = event.event.player
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
            is MessageWrapper -> {
                plugin.logger.info("MessageWrapper detected!")
                event.originalMessage.author.openPrivateChannel().queue({p ->
                    plugin.logger.info("RestAction returned: $p")
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
                plugin.logger.info("Continuing non-blocking thread")
            }
        }
    }

    // SERVERLIST - List all players currently online on the server
    @DiscordExclusiveCommand
    @BotCommand(usage="", description = "List all Minecraft players currently online on the server",
            relayTriggerMessage = false)
    private fun serverList(event: IEventWrapper) {
        if (event !is MessageWrapper) return
        plugin.logDebug("user ${event.originalMessage.author.name} has requested a listing of online players")

        val players = plugin.getOnlinePlayers()
        if (players.isEmpty()) {
            plugin.sendToDiscord("Nobody is currently online.", event.channel)
            return
        }

        val response = players.joinToString("\n", "The following players are currently online:\n```\n", "\n```")
        plugin.sendToDiscord(response, event.channel)
    }
}