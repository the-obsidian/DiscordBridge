package gg.obsidian.discordbridge.commands.controllers

import gg.obsidian.discordbridge.Config
import gg.obsidian.discordbridge.Plugin
import gg.obsidian.discordbridge.commands.*
import gg.obsidian.discordbridge.commands.annotations.*
import gg.obsidian.discordbridge.discord.Connection
import gg.obsidian.discordbridge.utils.MarkdownToMinecraftSeralizer
import gg.obsidian.discordbridge.utils.Script
import gg.obsidian.discordbridge.utils.UtilFunctions.noSpace
import gg.obsidian.discordbridge.utils.UtilFunctions.stripColor
import gg.obsidian.discordbridge.utils.UtilFunctions.toDiscordChatMessage
import gg.obsidian.discordbridge.utils.UtilFunctions.toMinecraftChatMessage
import net.dv8tion.jda.core.Permission
import org.bukkit.Bukkit
import java.lang.reflect.Method
import java.util.*
import java.util.logging.Level
import gg.obsidian.discordbridge.Config as cfg
import org.bukkit.ChatColor as CC

/**
 * A class that manages an assortment of IBotControllers and allows dynamic access to a configurable assortment
 * of their commands
 *
 * @param plugin a reference to the base Plugin object
 * @see IBotController
 */
class BotControllerManager(val plugin: Plugin) {

    private val commands: MutableMap<String, Command> = mutableMapOf()
    private val controllers: MutableMap<Class<out IBotController>, IBotController> = mutableMapOf()

    /**
     * Adds an IBotController to the manager.
     *
     * @param controller the IBotController to add
     * @param discordExclusive (optional) whether this BotControllerManager instance should have access to this IBotController's
     * methods annotated by DiscordExclusiveCommand (defaults false)
     * @param minecraftExclusive (optional) whether this BotControllerManager instance should have access to this IBotController's
     * methods annotated by MinecraftExclusiveCommand (defaults false)
     * @param chatExclusive (optional) whether this BotControllerManager instance should have access to this IBotController's
     * methods annotated by ChatExclusiveCommand (defaults false)
     */
    fun registerController(controller: IBotController, discordExclusive: Boolean = false,
                           minecraftExclusive: Boolean = false, chatExclusive: Boolean = false) {
        controllers.put(controller.javaClass, controller)
        val controllerClass = controller.javaClass

        for (method in controllerClass.declaredMethods) {
            val annotation = method.getAnnotation(BotCommand::class.java)
            if (annotation != null
                    && (discordExclusive || method.getAnnotation(DiscordExclusiveCommand::class.java) == null)
                    && (minecraftExclusive || method.getAnnotation(MinecraftExclusiveCommand::class.java) == null)
                    && (chatExclusive || method.getAnnotation(ChatExclusiveCommand::class.java) == null)) {
                registerControllerMethod(controllerClass, method, annotation)
            }
        }
    }

    /**
     * Registers an individual method from the IBotController
     *
     * @param controllerClass the Java class of the IBotController
     * @param method the method to register
     * @param annotation the BotCommand annotation object of the method
     */
    private fun registerControllerMethod(controllerClass: Class<*>, method: Method, annotation: BotCommand) {
        val commandName = if (annotation.name.isEmpty()) method.name.toLowerCase() else annotation.name
        val usage = annotation.usage
        val methodParameters = method.parameters

        if (methodParameters.isEmpty() || !methodParameters[0].type.isAssignableFrom(IEventWrapper::class.java)) return

        method.isAccessible = true
        val parameters = (1..methodParameters.size - 1).mapTo(ArrayList<Class<*>>()) { methodParameters[it].type }
        val isTagged: Boolean = method.getAnnotation(TaggedResponse::class.java) != null
        val isPrivate: Boolean = method.getAnnotation(PrivateResponse::class.java) != null
        val command = Command(commandName, usage, annotation.description, parameters, annotation.relayTriggerMessage,
                annotation.squishExcessArgs, isTagged, isPrivate, controllerClass, method)
        commands.put(command.name, command)
    }

    /**
     * Reads an incoming message and attempts to parse and execute a command.
     *
     * @param event the incoming event object
     * @return false if a command execution attempt failed, true otherwise
     */
    fun dispatchMessage(event: IEventWrapper): Boolean {

        // Short circuit if event was a Minecraft command
        if (event is MinecraftCommandWrapper) {
            val command = commands[event.command.name]
            if (command == null) {
                commandNotFound(event, event.command.name)
                return true
            }
            return invokeCommand(command, controllers, event, event.args.asList().toTypedArray())
        }

        // Short circuit scripted responses
        if (scriptedResponse(event)) return true

        // <prefix>command
        if (Config.COMMAND_PREFIX.isNotBlank() && event.rawMessage.startsWith(Config.COMMAND_PREFIX)) {
            val split = event.rawMessage.replaceFirst(Config.COMMAND_PREFIX, "").trim().split("\\s+".toRegex()).toTypedArray()
            return parseCommand(event, split, false)
        }

        // @<mention> command from Minecraft
        if (event is AsyncPlayerChatEventWrapper && event.rawMessage.startsWith("@${Config.USERNAME.noSpace()} ")) {
            val split = event.rawMessage.replaceFirst("@${Config.USERNAME.noSpace()} ", "").trim().split("\\s+".toRegex()).toTypedArray()
            return parseCommand(event, split, true)
        }

        // @<mention> command from Discord
        if (event is MessageWrapper && event.rawMessage.startsWith(Connection.JDA.selfUser.asMention + " ")) {
            val split = event.rawMessage.replaceFirst(Connection.JDA.selfUser.asMention + " ", "").trim().split("\\s+".toRegex()).toTypedArray()
            return parseCommand(event, split, true)
        }

        // Just relay the message if it is neither
        relay(event, true)
        return true
    }

    /**
     * Attempt to parse and execute a command from an input string
     *
     * @param event the original event object
     * @param args the input string broken up into an array of words, with the command as the first element
     * @param defaultToTalk if true, failure to find a command to execute will execute Talk with Cleverbot using the
     * full string as an argument. If false, failure will do nothing and the method call will return false.
     */
    private fun parseCommand(event: IEventWrapper, args: Array<String>, defaultToTalk: Boolean): Boolean {
        val commandName = args[0].toLowerCase()
        if (commandName == "") return true
        var command = commands[commandName]

        if (command == null) {
            // Attempt to run as a server command if sent from Discord
            if (event is MessageWrapper && event.originalMessage.member.hasPermission(Permission.ADMINISTRATOR))
                if (serverCommand(event, args)) return true

            if (defaultToTalk) command = commands["talk"]
            if (command == null) {
                commandNotFound(event, commandName)
                return false
            }
        }

        val slicedArgs = if (args.size > 1) args.slice(1 until args.size).toTypedArray() else arrayOf()
        return invokeCommand(command, controllers, event, slicedArgs)
    }

    /**
     * Looks for a scripted trigger and returns a respective response
     *
     * @param event the incoming event object
     * @return true if a trigger was found and successfully responded to, false otherwise
     */
    private fun scriptedResponse(event: IEventWrapper): Boolean {
        val responses = plugin.script.data.getList("responses").checkItemsAre<Script>() ?: return false

        var response: Script? = null
        for (r in responses) {
            var arg = event.rawMessage
            val triggerDis = r.triggerDis ?: ""
            val triggerMC = r.triggerMC ?: ""
            val ignorecase = !(r.caseSensitive != null && r.caseSensitive)
            val startswith = r.startsWith != null && r.startsWith
            val requiresmention = r.requiresMention != null && r.requiresMention

            if (requiresmention) {
                if (arg.startsWith(Connection.JDA.selfUser.asMention, true))
                    arg = arg.replaceFirst(Connection.JDA.selfUser.asMention, "").trimStart()
                else continue
            }

            when (event) {
                is AsyncPlayerChatEventWrapper -> if (startswith)
                    if (triggerMC.isNotEmpty() && arg.startsWith(triggerMC, ignorecase)) response = r
                    else if (triggerMC.isNotEmpty() && arg.equals(triggerMC, ignorecase)) response = r
                is MessageWrapper -> if (startswith)
                    if (triggerDis.isNotEmpty() && arg.startsWith(triggerDis, ignorecase)) response = r
                    else if (triggerDis.isNotEmpty() && arg.equals(triggerDis, ignorecase)) response = r
                else -> return false
            }
        }
        if (response == null) return false

        plugin.logDebug("user ${event.senderName} has triggered a scripted response")
        val responseDis: String? = response.responseDis
        val responseMC: String? = response.responseMC
        if (responseDis != null && responseDis.isNotBlank()) {
            var out = responseDis.replace("%u", event.senderAsMention)
            if (event is AsyncPlayerChatEventWrapper) {
                out = plugin.convertAtMentions(out)
                out = plugin.translateAliasesToDiscord(out)
            }
            plugin.sendToDiscord(out, event.channel)
        }
        if (responseMC != null && responseMC.isNotBlank() && event.isFromRelayChannel) {
            var out = responseMC.replace("%u", event.senderAsMention)
            if (event is MessageWrapper) {
                out = plugin.deconvertAtMentions(out)
                out = plugin.translateAliasesToMinecraft(out)
            }
            plugin.sendToMinecraft(out.toMinecraftChatMessage(Config.BOT_MC_USERNAME))
        }
        return true
    }

    /**
     * Attempts to invoke a discovered command
     *
     * @param command the command to invoke
     * @param instances a map of IBotController instances accessed by their Java classes
     * @param event the incoming event object
     * @param args an array of String arguments to pass to the command method
     * @return false if the command invocation has invalid arguments, true otherwise
     */
    private fun invokeCommand(command: Command, instances: Map<Class<out IBotController>, IBotController>,
                              event: IEventWrapper, args: Array<String>): Boolean {
        // Relay the trigger if applicable
        if (command.relayTriggerMessage) relay(event, false)

        var squishedArgs = args

        when {
            // Check for permission
            event is AsyncPlayerChatEventWrapper && !event.event.player.hasPermission("discordbridge.${command.name}") -> {
                commandRestricted(event)
                return true
            }
            event is MinecraftCommandWrapper && !event.sender.hasPermission("discordbridge.${command.name}") -> {
                commandRestricted(event)
                return true
            }

            // Short circuit "help"
            command.name == "help" -> {
                try {
                    val response = command.commandMethod.invoke(instances[command.controllerClass], event, commands, instances) as? String ?: return true
                    respond(event, command, response)
                    return true
                } catch (e: IllegalArgumentException) {
                    commandWrongParameterCount(event, command.name, command.usage, 2, command.parameters.size)
                    return false
                } catch (e: Exception) {
                    commandException(event, e)
                    return true
                }
            }

            // If command is squishy, pack excess args into final string
            command.squishExcessArgs -> {
                if (args.size == 1)
                    squishedArgs = arrayOf(args.joinToString(" "))
                else {
                    squishedArgs = args.sliceArray(0 until command.parameters.size)
                    squishedArgs[command.parameters.size-1] = args.sliceArray(command.parameters.size until args.size).joinToString(" ")
                }
            }

            // Fail if wrong number of arguments
            squishedArgs.size != command.parameters.size -> {
                commandWrongParameterCount(event, command.name, command.usage, squishedArgs.size, command.parameters.size)
                return false
            }
        }

        // Package arguments to send to method
        val arguments = arrayOfNulls<Any>(command.parameters.size + 1)
        arguments[0] = event
        val paramRange = if (command.squishExcessArgs && squishedArgs.size > command.parameters.size)
            IntRange(0, command.parameters.size - 1) else command.parameters.indices
        for (i in paramRange) {
            val parameterClass = command.parameters[i]

            try {
                arguments[i + 1] = parseArgument(parameterClass, squishedArgs[i])
            } catch (ignored: IllegalArgumentException) {
                commandWrongParameterType(event, command.name, command.usage, i, squishedArgs[i].javaClass, parameterClass)
                return false
            }
        }

        // Invoke the method
        try {
            val response = command.commandMethod.invoke(instances[command.controllerClass], *arguments) as? String ?: return true
            respond(event, command, response)
            return true
        } catch (e: IllegalArgumentException) {
            commandWrongParameterCount(event, command.name, command.usage, squishedArgs.size, command.parameters.size)
            return false
        } catch (e: Exception) {
            commandException(event, e)
            return true
        }

    }

    /**
     * Attempts to run a default or installed chat command as server
     *
     * @param event the incoming event object
     * @param args the arguments associated with the command, space-delimited
     */
    private fun serverCommand(event: IEventWrapper, args: Array<String>): Boolean {
            val sender = DiscordCommandSender(event.channel)
            val commandName = args[0].toLowerCase()
            when {
                DefaultCommands.minecraft.contains(commandName) -> {
                    plugin.logDebug("Discord user ${event.senderName} invoked Minecraft command '${args.joinToString(" ")}'")
                    Bukkit.getServer().dispatchCommand(sender, args.joinToString(" "))
                    return true
                }

                DefaultCommands.bukkit.contains(commandName) -> {
                    plugin.logDebug("Discord user ${event.senderName} invoked Bukkit command '${args.joinToString(" ")}'")
                    Bukkit.getServer().dispatchCommand(sender, args.joinToString(" "))
                    return true
                }

                DefaultCommands.spigot.contains(commandName) -> {
                    plugin.logDebug("Discord user ${event.senderName} invoked Spigot command '${args.joinToString(" ")}'")
                    Bukkit.getServer().dispatchCommand(sender, args.joinToString(" "))
                    return true
                }

                else -> {
                    val pluginCommand = Bukkit.getServer().getPluginCommand(commandName)
                    if (pluginCommand != null) {
                        plugin.logDebug("Discord user ${event.senderName} invoked ${pluginCommand.plugin.name} command '${args.joinToString(" ")}'")
                        pluginCommand.execute(sender, commandName, args.sliceArray(1 until args.size))
                        return true
                    }
                }
            }
        return false
    }

    /**
     * Sends the response of a successful command invocation to its respective medium
     *
     * @param event the incoming event object
     * @param command the command that was invoked
     * @param response the output string of the invoked command
     */
    private fun respond(event: IEventWrapper, command: Command, response: String) {
        var modifiedResponse = response
        when (event) {
            is AsyncPlayerChatEventWrapper -> {
                if (command.isPrivate) {
                    modifiedResponse = MarkdownToMinecraftSeralizer().toMinecraft(plugin.pegDownProc.parseMarkdown(modifiedResponse.toCharArray()))
                    modifiedResponse = "${CC.ITALIC}${CC.GRAY}${Config.BOT_MC_USERNAME.stripColor()} whispers to you: " +
                            modifiedResponse
                    event.event.player.sendMessage(modifiedResponse)
                    return
                }
                if (command.isTagged) modifiedResponse = "${event.senderAsMention} | $modifiedResponse"
                val mcModifiedResponse = MarkdownToMinecraftSeralizer().toMinecraft(plugin.pegDownProc.parseMarkdown(modifiedResponse.toCharArray()))
                plugin.sendToMinecraft(mcModifiedResponse.toMinecraftChatMessage(Config.BOT_MC_USERNAME))

                modifiedResponse = plugin.convertAtMentions(modifiedResponse)
                modifiedResponse = plugin.translateAliasesToDiscord(modifiedResponse)
                plugin.sendToDiscord(modifiedResponse, event.channel)
                return
            }
            is MessageWrapper -> {
                if (command.isPrivate) {
                    event.originalMessage.author.openPrivateChannel().queue({ p -> p.sendMessage(modifiedResponse).queue() })
                    return
                }
                if (command.isTagged) modifiedResponse = "${event.senderAsMention} | $modifiedResponse"
                plugin.sendToDiscord(modifiedResponse, event.channel)

                modifiedResponse = modifiedResponse.toMinecraftChatMessage(Config.BOT_MC_USERNAME)
                modifiedResponse = plugin.deconvertAtMentions(modifiedResponse)
                modifiedResponse = plugin.translateAliasesToMinecraft(modifiedResponse)
                modifiedResponse = MarkdownToMinecraftSeralizer().toMinecraft(plugin.pegDownProc.parseMarkdown(modifiedResponse.toCharArray()))
                plugin.sendToMinecraft(modifiedResponse)
                return
            }
            is MinecraftCommandWrapper -> {
                if (command.isPrivate || command.isTagged) {
                    modifiedResponse = MarkdownToMinecraftSeralizer().toMinecraft(plugin.pegDownProc.parseMarkdown(modifiedResponse.toCharArray()))
                    modifiedResponse = "${CC.ITALIC}${CC.GRAY}${Config.BOT_MC_USERNAME.stripColor()} whispers to you: " +
                            modifiedResponse
                    event.sender.sendMessage(modifiedResponse)
                    return
                }
                val mcModifiedResponse = MarkdownToMinecraftSeralizer().toMinecraft(plugin.pegDownProc.parseMarkdown(modifiedResponse.toCharArray()))
                plugin.sendToMinecraft(mcModifiedResponse.toMinecraftChatMessage(Config.BOT_MC_USERNAME))

                modifiedResponse = plugin.convertAtMentions(modifiedResponse)
                modifiedResponse = plugin.translateAliasesToDiscord(modifiedResponse)
                plugin.sendToDiscord(modifiedResponse, event.channel)
                return
            }
        }
    }

    /**
     * Attempts to relay the message between Minecraft and Discord, if applicable
     *
     * @param event the incoming event object
     * @param logIgnore if true, and if this message was from Discord, a debug log message will be generated if the
     * message is not relayed to Minecraft
     */
    private fun relay(event: IEventWrapper, logIgnore: Boolean) {
        when (event) {
            is AsyncPlayerChatEventWrapper -> {
                var worldname = event.event.player.world.name

                // Get world alias if Multiverse is installed
                if (plugin.isMultiverseInstalled) {
                    val worldProperties = plugin.worlds!!.data.get("worlds.$worldname")
                    val cls = Class.forName("com.onarandombox.MultiverseCore.WorldProperties")
                    val meth: Method = cls.getMethod("getAlias")
                    val alias = meth.invoke(worldProperties)
                    if (alias is String) worldname = alias
                }

                var formattedMessage = event.message.toDiscordChatMessage(event.senderName, worldname)
                formattedMessage = plugin.convertAtMentions(formattedMessage)
                formattedMessage = plugin.translateAliasesToDiscord(formattedMessage)
                plugin.sendToDiscord(formattedMessage, Connection.getRelayChannel())
            }
            is MessageWrapper -> {
                if (event.isFromRelayChannel) {
                    plugin.logDebug("Broadcasting message from Discord to Minecraft as user ${event.senderName}")
                    var formattedMessage = event.message.toMinecraftChatMessage(event.senderName)
                    formattedMessage = plugin.deconvertAtMentions(formattedMessage)
                    formattedMessage = plugin.translateAliasesToMinecraft(formattedMessage)
                    formattedMessage = MarkdownToMinecraftSeralizer().toMinecraft(plugin.pegDownProc.parseMarkdown(formattedMessage.toCharArray()))
                    plugin.sendToMinecraft(formattedMessage)
                } else if (logIgnore) plugin.logDebug("Not relaying message from Discord: channel does not match")
            }
            else -> return // slash commands do not get relayed
        }
    }

    /**
     * Attempts to parse a string parameter to an expected value type
     *
     * @param parameterClass the expected type of the parameter
     * @param value a string representation of the value to be parsed
     */
    private fun parseArgument(parameterClass: Class<*>, value: String): Any {
        try {
            when (parameterClass) {
                String::class.java -> return value
                Int::class.javaPrimitiveType, Int::class.java -> return Integer.valueOf(value)
                Long::class.javaPrimitiveType, Long::class.java -> return java.lang.Long.valueOf(value)
                Boolean::class.javaPrimitiveType, Boolean::class.java -> return parseBooleanArgument(value)
                Float::class.javaPrimitiveType, Float::class.java -> return java.lang.Float.valueOf(value)
                Double::class.javaPrimitiveType, Double::class.java -> return java.lang.Double.valueOf(value)
                else -> throw IllegalArgumentException()
            }
        } catch (ignored: NumberFormatException) {
            throw IllegalArgumentException()
        }
    }

    /**
     * Attempts to parse a string parameter to a Boolean type
     *
     * @param value a string representation of the value to be parsed
     */
    private fun parseBooleanArgument(value: String): Boolean {
        when (value.toLowerCase()) {
            "yes", "true" -> return true
            "no", "false" -> return false
            else -> {
                val integerValue = Integer.valueOf(value)!!
                when (integerValue) {
                    1 -> return true
                    0 -> return false
                    else -> throw IllegalArgumentException()
                }
            }
        }
    }

    /**
     * A function to assert that all the items in a given list are of a specific type
     */
    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Any> List<*>.checkItemsAre() = if (all { it is T }) this as List<T> else null

    /**
     * Handler for if command syntax is given but no matching command is found
     *
     * @param event the incoming event object
     * @param commandName the name of the command that was not found
     */
    private fun commandNotFound(event: IEventWrapper, commandName: String) {
        when (event) {
            is MessageWrapper ->
                event.channel.sendMessage(
                        "${event.senderAsMention} | I don't seem to have a command called '$commandName'. " +
                                "See '${Connection.JDA.selfUser.asMention} help' ${orPrefixHelp()}for the commands I do have.").queue()
            is AsyncPlayerChatEventWrapper ->
                event.event.player.sendMessage("${CC.ITALIC}${CC.GRAY}${Config.BOT_MC_USERNAME.stripColor()} whispers to you: " +
                        "I don't seem to have a command called '$commandName'. " +
                        "See '@${Config.BOT_MC_USERNAME.stripColor()} help' ${orPrefixHelp()}for the commands I do have.")
            is MinecraftCommandWrapper ->
                event.sender.sendMessage("${CC.ITALIC}${CC.GRAY}${Config.BOT_MC_USERNAME.stripColor()} whispers to you: " +
                        "I don't seem to have a command called '$commandName'. " +
                        "See '@${Config.BOT_MC_USERNAME.stripColor()} help' ${orPrefixHelp()}for the commands I do have.")
        }

    }

    /**
     * Handler for when a command is invoked with the wrong number of parameters
     *
     * @param event the incoming event object
     * @param commandName the name of the command that was invoked
     * @param usage the usage description of the command
     * @param given how many arguments were passed in the invocation
     * @param required how many arguments were expected
     */
    private fun commandWrongParameterCount(event: IEventWrapper, commandName: String, usage: String, given: Int, required: Int) {
        when (event) {
            is MessageWrapper ->
                event.channel.sendMessage(
                        "${event.senderAsMention} | I didn't seem to get the correct number of arguments for that command. " +
                                "I got $given from you, but I need $required. " +
                                "(Usage: $commandName $usage)").queue()
            is AsyncPlayerChatEventWrapper ->
                event.event.player.sendMessage("${CC.ITALIC}${CC.GRAY}${Config.BOT_MC_USERNAME.stripColor()} whispers to you: " +
                        "I didn't seem to get the correct number of arguments for that command. " +
                        "I got $given from you, but I need $required. " +
                        "(Usage: $commandName $usage)")
            is MinecraftCommandWrapper ->
                event.sender.sendMessage("${CC.ITALIC}${CC.GRAY}${Config.BOT_MC_USERNAME.stripColor()} whispers to you: " +
                        "I didn't seem to get the correct number of arguments for that command. " +
                        "I got $given from you, but I need $required. " +
                        "(Usage: $commandName $usage)")
        }
    }

    /**
     * Handler for when one of the arguments in a command invocation was of an unexpected type
     *
     * @param event the incoming event object
     * @param name the name of the command that was invoked
     * @param usage the usage description of the command
     * @param index the index of the offending argument
     * @param actualType the actual type of the offending argument during invocation
     * @param expectedType the argument type that was expected
     */
    private fun commandWrongParameterType(event: IEventWrapper, name: String, usage: String, index: Int,
                                          actualType: Class<*>, expectedType: Class<*>) {
        when (event) {
            is MessageWrapper ->
                event.channel.sendMessage(
                        "${event.senderAsMention} | One of the arguments for that command doesn't seem to be the right type. " +
                                "Argument $index seems like ${actualType.simpleName}, but I need ${expectedType.simpleName}. " +
                                "(Usage: $name $usage)").queue()
            is AsyncPlayerChatEventWrapper ->
                event.event.player.sendMessage("${CC.ITALIC}${CC.GRAY}${Config.BOT_MC_USERNAME.stripColor()} whispers to you: " +
                        "One of the arguments for that command doesn't seem to be the right type. " +
                        "Argument $index seems like ${actualType.simpleName}, but I need ${expectedType.simpleName}. " +
                        "(Usage: $name $usage)")
            is MinecraftCommandWrapper ->
                event.sender.sendMessage("${CC.ITALIC}${CC.GRAY}${Config.BOT_MC_USERNAME.stripColor()} whispers to you: " +
                        "One of the arguments for that command doesn't seem to be the right type. " +
                        "Argument $index seems like ${actualType.simpleName}, but I need ${expectedType.simpleName}. " +
                        "(Usage: $name $usage)")
        }
    }

    /**
     * Handler for when an invoker invokes a command they do not have permission to use
     *
     * @param event the incoming event object
     */
    private fun commandRestricted(event: IEventWrapper) {
        when (event) {
            is MessageWrapper ->
                event.channel.sendMessage("${event.senderAsMention} | Sorry, you don't have permission to use that command.").queue()
            is AsyncPlayerChatEventWrapper ->
                event.event.player.sendMessage("${CC.ITALIC}${CC.GRAY}${Config.BOT_MC_USERNAME.stripColor()} whispers to you: " +
                        "Sorry, you don't have permission to use that command.")
            is MinecraftCommandWrapper ->
                event.sender.sendMessage("${CC.ITALIC}${CC.GRAY}${Config.BOT_MC_USERNAME.stripColor()} whispers to you: " +
                        "Sorry, you don't have permission to use that command.")
        }
    }

    /**
     * Handler for when a command invocation throws an uncaught exception
     *
     * @param event the incoming event object
     * @param throwable the uncaught exception
     */
    private fun commandException(event: IEventWrapper, throwable: Throwable) {
        plugin.logger.log(Level.SEVERE, "Command with content ${event.rawMessage} threw an exception.", throwable)
        when (event) {
            is MessageWrapper ->
                event.channel.sendMessage(
                        "${event.senderAsMention} | Ouch! That command threw an exception. Sorry about that... " +
                                "Have an admin check the logs for more info.").queue()
            is AsyncPlayerChatEventWrapper ->
                event.event.player.sendMessage("${CC.ITALIC}${CC.GRAY}${Config.BOT_MC_USERNAME.stripColor()} whispers to you: " +
                        "Ouch! That command threw an exception. Sorry about that... " +
                        "Have an admin check the logs for more info.")
            is MinecraftCommandWrapper ->
                event.sender.sendMessage("${CC.ITALIC}${CC.GRAY}${Config.BOT_MC_USERNAME.stripColor()} whispers to you: " +
                        "Ouch! That command threw an exception. Sorry about that... " +
                        "Have an admin check the logs for more info.")
        }
    }

    /**
     * Shortcut method for adding "or <prefix>help " to the CommandNotFound output if a COMMAND_PREFIX is set in config
     */
    private fun orPrefixHelp(): String = if (Config.COMMAND_PREFIX != "") "or ${Config.COMMAND_PREFIX}help " else ""

    /**
     * Represents a command that the bot can execute
     *
     * @param name the name of the command (not necessarily the name of the method called)
     * @param usage the usage description of the method
     * @param description a description of the command's function
     * @param parameters a List of the command arguments' expected Java class types
     * @param relayTriggerMessage whether the message that triggered this command should be relayed
     * @param squishExcessArgs whether this command should combine excess arguments into one
     * @param isTagged if the output of this command should be prepended with "@invokerName | "
     * @param isPrivate if the output of this command should be sent via DM/PM to the invoker
     * @param controllerClass the Java class type of the IBotController that defines this command
     * @param commandMethod the method that is called when this command is invoked
     */
    data class Command(
            val name: String,
            val usage: String,
            val description: String,
            val parameters: List<Class<*>>,
            val relayTriggerMessage: Boolean,
            val squishExcessArgs: Boolean,
            val isTagged: Boolean,
            val isPrivate: Boolean,
            val controllerClass: Class<*>,
            val commandMethod: Method
    )

    private object DefaultCommands {
        val minecraft : List<String> = listOf(
                "advancement",
                "ban",
                "blockdata",
                "clear",
                "clone",
                //"data",
                //"datapack",
                "debug",
                "defaultgamemode",
                "deop",
                "difficulty",
                "effect",
                "enchant",
                "entitydata",
                //"experience",
                "execute",
                "fill",
                "function",
                "gamemode",
                "gamerule",
                "give",
                "help",
                "kick",
                "kill",
                "list",
                "locate",
                "me",
                "op",
                "pardon",
                "particle",
                "playsound",
                "publish",
                "recipe",
                "reload",
                "replaceitem",
                "save",
                "say",
                "scoreboard",
                "seed",
                "setblock",
                "setidletimeout",
                "setmaxplayers",
                "setworldspawn",
                "spawnpoint",
                "spreadplayers",
                "stats",
                "stop",
                "stopsound",
                "summon",
                "teleport",
                "tell",
                //"tag",
                //"team",
                "tellraw",
                "testfor",
                "testforblock",
                "testforblocks",
                "tickingarea",
                "time",
                "title",
                "toggledownfall",
                "tp",
                "transferserver",
                "trigger",
                "weather",
                "whitelist",
                "worldborder",
                "wsserver"
        )

        val bukkit : List<String> = listOf(
                "version",
                "plugins",
                "help",
                "reload",
                "timings"
        )

        val spigot:List<String> = listOf(
                "restart",
                "tps"
        )
    }
}
