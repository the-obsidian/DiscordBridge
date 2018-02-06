package gg.obsidian.discordbridge.commands.controllers

import gg.obsidian.discordbridge.DiscordBridge
import gg.obsidian.discordbridge.commands.*
import gg.obsidian.discordbridge.commands.annotations.*
import gg.obsidian.discordbridge.discord.Connection
import gg.obsidian.discordbridge.util.Cfg
import gg.obsidian.discordbridge.util.MarkdownToMinecraftSeralizer
import gg.obsidian.discordbridge.util.Script
import gg.obsidian.discordbridge.util.UtilFunctions.noSpace
import gg.obsidian.discordbridge.util.UtilFunctions.stripColor
import gg.obsidian.discordbridge.util.UtilFunctions.toDiscordChatMessage
import gg.obsidian.discordbridge.util.UtilFunctions.toMinecraftChatMessage
import net.dv8tion.jda.core.Permission
import java.lang.reflect.Method
import java.util.*
import gg.obsidian.discordbridge.util.ChatColor as CC

/**
 * A class that manages an assortment of IBotControllers and allows dynamic access to a configurable assortment
 * of their commands
 * 
 * @see IBotController
 */
class BotControllerManager {

    private val commands: MutableMap<String, Command> = mutableMapOf()
    private val controllers: MutableMap<Class<out IBotController>, IBotController> = mutableMapOf()

    // =============================================
    // =============== SETUP METHODS ===============

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
        val commandAliases = annotation.aliases
        val usage = annotation.usage
        val methodParameters = method.parameters

        if (methodParameters.isEmpty() || !methodParameters[0].type.isAssignableFrom(IEventWrapper::class.java)) return

        method.isAccessible = true
        val parameters = (1 until methodParameters.size).mapTo(ArrayList<Class<*>>()) { methodParameters[it].type }
        val isTagged: Boolean = method.getAnnotation(TaggedResponse::class.java) != null
        val isPrivate: Boolean = method.getAnnotation(PrivateResponse::class.java) != null
        val command = Command(commandAliases, usage, annotation.desc, annotation.help, parameters, annotation.relayTriggerMessage,
                annotation.squishExcessArgs, annotation.ignoreExcessArgs, isTagged, isPrivate, controllerClass, method)
        commands.put(command.aliases[0], command)
    }

    fun getCommands(): Map<String, Command> {
        return commands
    }

    // =============================================
    // ============== MESSAGE HANDLING =============

    /**
     * Reads an incoming message and attempts to parse and execute a command.
     *
     * @param event the incoming event object
     * @return false if a command execution attempt failed, true otherwise
     */
    fun dispatchMessage(event: IEventWrapper): Boolean {

        // Short circuit if event was a Minecraft command
        if (event is MinecraftCommandWrapper) {
            val command = commands[event.commandName] ?: return true
            return invokeBotCommand(command, controllers, event, event.args.asList().toTypedArray())
        }

        // Short circuit scripted responses
        if (sendScriptedResponse(event)) return true

        // <prefix>command
        if (DiscordBridge.getConfig(Cfg.CONFIG).getString("command-prefix", "").isNotBlank() && event.rawMessage.startsWith(DiscordBridge.getConfig(Cfg.CONFIG).getString("command-prefix", ""))) {
            val split = event.rawMessage.replaceFirst(DiscordBridge.getConfig(Cfg.CONFIG).getString("command-prefix", ""), "").trim().split("\\s+".toRegex()).toTypedArray()
            return parseCommand(event, split, false)
        }

        // @<mention> command from Minecraft
        if (event is MinecraftChatEventWrapper && event.rawMessage.startsWith("@${DiscordBridge.getConfig(Cfg.CONFIG).getString("username", "DiscordBridge").noSpace()} ")) {
            val split = event.rawMessage.replaceFirst("@${DiscordBridge.getConfig(Cfg.CONFIG).getString("username", "DiscordBridge").noSpace()} ", "").trim().split("\\s+".toRegex()).toTypedArray()
            return parseCommand(event, split, true)
        }

        // @<mention> command from Discord
        if (event is DiscordMessageWrapper && event.rawMessage.startsWith(Connection.JDA.selfUser.asMention + " ")) {
            val split = event.rawMessage.replaceFirst(Connection.JDA.selfUser.asMention + " ", "").trim().split("\\s+".toRegex()).toTypedArray()
            return parseCommand(event, split, true)
        }

        // Just relay the message if no command is found
        relay(event, true)
        return true
    }

    /**
     * Looks for a scripted trigger and returns a respective response
     *
     * @param event the incoming event object
     * @return true if a trigger was found and successfully responded to, false otherwise
     */
    private fun sendScriptedResponse(event: IEventWrapper): Boolean {
        val responses = DiscordBridge.getConfig(Cfg.SCRIPT).getList<HashMap<String, Any>>("responses").castTo({Script(it)})

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
                is MinecraftChatEventWrapper -> if (startswith)
                    if (triggerMC.isNotEmpty() && arg.startsWith(triggerMC, ignorecase)) response = r
                    else if (triggerMC.isNotEmpty() && arg.equals(triggerMC, ignorecase)) response = r
                is DiscordMessageWrapper -> if (startswith)
                    if (triggerDis.isNotEmpty() && arg.startsWith(triggerDis, ignorecase)) response = r
                    else if (triggerDis.isNotEmpty() && arg.equals(triggerDis, ignorecase)) response = r
                else -> return false
            }
        }
        if (response == null) return false

        DiscordBridge.logDebug("user ${event.senderName} has triggered a scripted response")
        val responseDis: String? = response.responseDis
        val responseMC: String? = response.responseMC
        if (responseDis != null && responseDis.isNotBlank()) {
            var out = responseDis.replace("%u", event.senderAsMention)
            if (event is MinecraftChatEventWrapper) {
                out = DiscordBridge.convertAtMentions(out)
                out = DiscordBridge.translateAliasesToDiscord(out)
            }
            DiscordBridge.sendToDiscord(out, event.channel)
        }
        if (responseMC != null && responseMC.isNotBlank() && event.isFromRelayChannel) {
            var out = responseMC.replace("%u", event.senderAsMention)
            if (event is DiscordMessageWrapper) {
                out = DiscordBridge.deconvertAtMentions(out)
                out = DiscordBridge.translateAliasesToMinecraft(out)
            }
            DiscordBridge.sendToMinecraft(out.toMinecraftChatMessage(DiscordBridge.getConfig(Cfg.CONFIG).getString("username", "DiscordBridge")))
        }
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
            if (event is DiscordMessageWrapper && event.originalMessage.member.hasPermission(Permission.ADMINISTRATOR))
                if (invokeServerCommand(event, args)) return true

            if (defaultToTalk) command = commands["talk"]
            if (command == null) return false
        }

        val slicedArgs = if (args.size > 1) args.slice(1 until args.size).toTypedArray() else arrayOf()
        return invokeBotCommand(command, controllers, event, slicedArgs)
    }

    /**
     * Attempts to run a DiscordBridge command
     *
     * @param command the command to invoke
     * @param instances a map of IBotController instances accessed by their Java classes
     * @param event the incoming event object
     * @param args an array of String arguments to pass to the command method
     * @return false if the command invocation has invalid arguments, true otherwise
     */
    private fun invokeBotCommand(command: Command, instances: Map<Class<out IBotController>, IBotController>,
                                 event: IEventWrapper, args: Array<String>): Boolean {
        // Relay the trigger if applicable
        if (command.relayTriggerMessage) relay(event, false)

        var squishedArgs = args

        when {
            // Check for permission
            event is MinecraftChatEventWrapper && !event.player.hasPermission("discordbridge.${command.aliases[0]}") -> {
                commandRestricted(event)
                return true
            }
            event is MinecraftCommandWrapper && !event.sender.hasPermission("discordbridge.${command.aliases[0]}") -> {
                commandRestricted(event)
                return true
            }

            // Short circuit "help"
            command.aliases[0] == "help" -> {
                try {
                    val response = command.commandMethod.invoke(instances[command.controllerClass], event, commands, instances) as? String ?: return true
                    sendBotCommandOutput(event, command, response)
                    return true
                } catch (e: IllegalArgumentException) {
                    commandWrongParameterCount(event, command.aliases[0], command.usage, 2, command.parameters.size)
                    return false
                } catch (e: Exception) {
                    commandException(event, e)
                    return true
                }
            }

            // If command is squishy, pack excess args into final string
            command.squishExcessArgs -> {
                if (command.parameters.size == 1)
                    squishedArgs = arrayOf(args.joinToString(" "))
                else {
                    squishedArgs = args.sliceArray(0 until command.parameters.size-1)
                    squishedArgs[command.parameters.size-1] = args.sliceArray(command.parameters.size-1 until args.size).joinToString(" ")
                }
            }

            // Fail if wrong number of arguments
            !command.ignoreExcessArgs && squishedArgs.size != command.parameters.size -> {
                commandWrongParameterCount(event, command.aliases[0], command.usage, squishedArgs.size, command.parameters.size)
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
                commandWrongParameterType(event, command.aliases[0], command.usage, i, squishedArgs[i].javaClass, parameterClass)
                return false
            }
        }

        // Invoke the method
        try {
            val response = command.commandMethod.invoke(instances[command.controllerClass], *arguments) as? String ?: return true
            sendBotCommandOutput(event, command, response)
            return true
        } catch (e: IllegalArgumentException) {
            commandWrongParameterCount(event, command.aliases[0], command.usage, squishedArgs.size, command.parameters.size)
            return false
        } catch (e: Exception) {
            commandException(event, e)
            return true
        }

    }

    /**
     * Sends the response of a successful bot command invocation to its respective medium
     *
     * @param event the incoming event object
     * @param command the command that was invoked
     * @param response the output string of the invoked command
     */
    private fun sendBotCommandOutput(event: IEventWrapper, command: Command, response: String) {
        var modifiedResponse = response
        when (event) {
            is MinecraftChatEventWrapper -> {
                if (command.isPrivate) {
                    modifiedResponse = MarkdownToMinecraftSeralizer().toMinecraft(DiscordBridge.getPegDownProcessor().parseMarkdown(modifiedResponse.toCharArray()))
                    modifiedResponse = "${CC.ITALIC}${CC.GRAY}${DiscordBridge.getConfig(Cfg.CONFIG).getString("username", "DiscordBridge").stripColor()} whispers to you: " +
                            modifiedResponse
                    event.player.sendMessage(modifiedResponse)
                    return
                }
                if (command.isTagged) modifiedResponse = "${event.senderAsMention} | $modifiedResponse"
                val mcModifiedResponse = MarkdownToMinecraftSeralizer().toMinecraft(DiscordBridge.getPegDownProcessor().parseMarkdown(modifiedResponse.toCharArray()))
                DiscordBridge.sendToMinecraft(mcModifiedResponse.toMinecraftChatMessage(DiscordBridge.getConfig(Cfg.CONFIG).getString("username", "DiscordBridge")))

                modifiedResponse = DiscordBridge.convertAtMentions(modifiedResponse)
                modifiedResponse = DiscordBridge.translateAliasesToDiscord(modifiedResponse)
                DiscordBridge.sendToDiscord(modifiedResponse, event.channel)
                return
            }
            is DiscordMessageWrapper -> {
                if (command.isPrivate) {
                    event.originalMessage.author.openPrivateChannel().queue({ p -> p.sendMessage(modifiedResponse).queue() })
                    return
                }
                if (command.isTagged) modifiedResponse = "${event.senderAsMention} | $modifiedResponse"
                DiscordBridge.sendToDiscord(modifiedResponse, event.channel)

                modifiedResponse = modifiedResponse.toMinecraftChatMessage(DiscordBridge.getConfig(Cfg.CONFIG).getString("username", "DiscordBridge"))
                modifiedResponse = DiscordBridge.deconvertAtMentions(modifiedResponse)
                modifiedResponse = DiscordBridge.translateAliasesToMinecraft(modifiedResponse)
                modifiedResponse = MarkdownToMinecraftSeralizer().toMinecraft(DiscordBridge.getPegDownProcessor().parseMarkdown(modifiedResponse.toCharArray()))
                DiscordBridge.sendToMinecraft(modifiedResponse)
                return
            }
            is MinecraftCommandWrapper -> {
                if (command.isPrivate || command.isTagged) {
                    modifiedResponse = MarkdownToMinecraftSeralizer().toMinecraft(DiscordBridge.getPegDownProcessor().parseMarkdown(modifiedResponse.toCharArray()))
                    modifiedResponse = "${CC.ITALIC}${CC.GRAY}${DiscordBridge.getConfig(Cfg.CONFIG).getString("username", "DiscordBridge").stripColor()} whispers to you: " +
                            modifiedResponse
                    event.sender.sendMessage(modifiedResponse)
                    return
                }
                val mcModifiedResponse = MarkdownToMinecraftSeralizer().toMinecraft(DiscordBridge.getPegDownProcessor().parseMarkdown(modifiedResponse.toCharArray()))
                DiscordBridge.sendToMinecraft(mcModifiedResponse.toMinecraftChatMessage(DiscordBridge.getConfig(Cfg.CONFIG).getString("username", "DiscordBridge")))

                modifiedResponse = DiscordBridge.convertAtMentions(modifiedResponse)
                modifiedResponse = DiscordBridge.translateAliasesToDiscord(modifiedResponse)
                DiscordBridge.sendToDiscord(modifiedResponse, event.channel)
                return
            }
        }
    }

    /**
     * Attempts to run a Minecraft command
     *
     * @param event the incoming event object
     * @param args the arguments associated with the command, space-delimited
     */
    private fun invokeServerCommand(event: IEventWrapper, args: Array<String>): Boolean {
        val sender = DiscordCommandSender(event.senderName, event.channel)
        DiscordBridge.logDebug("Discord user ${event.senderName} invoked Minecraft command '${args.joinToString(" ")}'")
        DiscordBridge.getServer().dispatchCommand(sender, args.joinToString(" "))
        return true
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
            is MinecraftChatEventWrapper -> {
                var worldname = event.player.getWorld().getName()
                DiscordBridge.logger.info(worldname)

                // Get world alias if Multiverse is installed
                if (DiscordBridge.isMultiverse) {
                    val obj = DiscordBridge.mvWorlds.getObject("worlds.$worldname")
                    if (obj != null) {
                        val alias = (obj as Map<*, *>)["alias"]
                        if (alias is String && alias.isNotEmpty()) worldname = alias
                    }
                    else
                        DiscordBridge.logger.warning("Could not fetch world alias from config " +
                                "(did you `/discord reload` yet?)")
                }

                var formattedMessage = event.message.toDiscordChatMessage(event.senderName, worldname)
                formattedMessage = DiscordBridge.convertAtMentions(formattedMessage)
                formattedMessage = DiscordBridge.translateAliasesToDiscord(formattedMessage)
                DiscordBridge.sendToDiscord(formattedMessage, Connection.getRelayChannel())
            }
            is DiscordMessageWrapper -> {
                if (event.isFromRelayChannel) {
                    DiscordBridge.logDebug("Broadcasting message from Discord to Minecraft as user ${event.senderName}")
                    var formattedMessage = event.message.toMinecraftChatMessage(event.senderName)
                    formattedMessage = DiscordBridge.deconvertAtMentions(formattedMessage)
                    formattedMessage = DiscordBridge.translateAliasesToMinecraft(formattedMessage)
                    formattedMessage = MarkdownToMinecraftSeralizer().toMinecraft(DiscordBridge.getPegDownProcessor().parseMarkdown(formattedMessage.toCharArray()))
                    DiscordBridge.sendToMinecraft(formattedMessage)
                } else if (logIgnore) DiscordBridge.logDebug("Not relaying message from Discord: channel does not match")
            }
            else -> return // slash commands do not get relayed
        }
    }

    // =============================================
    // ============= Exception Handling ============

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
            is DiscordMessageWrapper ->
                event.channel.sendMessage(
                        "${event.senderAsMention} | I didn't seem to get the correct number of arguments for that command. " +
                                "I got $given from you, but I need $required. " +
                                "(Usage: $commandName $usage)").queue()
            is MinecraftChatEventWrapper ->
                event.player.sendMessage("${CC.ITALIC}${CC.GRAY}${DiscordBridge.getConfig(Cfg.CONFIG).getString("username", "DiscordBridge").stripColor()} whispers to you: " +
                        "I didn't seem to get the correct number of arguments for that command. " +
                        "I got $given from you, but I need $required. " +
                        "(Usage: $commandName $usage)")
            is MinecraftCommandWrapper ->
                event.sender.sendMessage("${CC.ITALIC}${CC.GRAY}${DiscordBridge.getConfig(Cfg.CONFIG).getString("username", "DiscordBridge").stripColor()} whispers to you: " +
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
            is DiscordMessageWrapper ->
                event.channel.sendMessage(
                        "${event.senderAsMention} | One of the arguments for that command doesn't seem to be the right type. " +
                                "Argument $index seems like ${actualType.simpleName}, but I need ${expectedType.simpleName}. " +
                                "(Usage: $name $usage)").queue()
            is MinecraftChatEventWrapper ->
                event.player.sendMessage("${CC.ITALIC}${CC.GRAY}${DiscordBridge.getConfig(Cfg.CONFIG).getString("username", "DiscordBridge").stripColor()} whispers to you: " +
                        "One of the arguments for that command doesn't seem to be the right type. " +
                        "Argument $index seems like ${actualType.simpleName}, but I need ${expectedType.simpleName}. " +
                        "(Usage: $name $usage)")
            is MinecraftCommandWrapper ->
                event.sender.sendMessage("${CC.ITALIC}${CC.GRAY}${DiscordBridge.getConfig(Cfg.CONFIG).getString("username", "DiscordBridge").stripColor()} whispers to you: " +
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
            is DiscordMessageWrapper ->
                event.channel.sendMessage("${event.senderAsMention} | Sorry, you don't have permission to use that command.").queue()
            is MinecraftChatEventWrapper ->
                event.player.sendMessage("${CC.ITALIC}${CC.GRAY}${DiscordBridge.getConfig(Cfg.CONFIG).getString("username", "DiscordBridge").stripColor()} whispers to you: " +
                        "Sorry, you don't have permission to use that command.")
            is MinecraftCommandWrapper ->
                event.sender.sendMessage("${CC.ITALIC}${CC.GRAY}${DiscordBridge.getConfig(Cfg.CONFIG).getString("username", "DiscordBridge").stripColor()} whispers to you: " +
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
        DiscordBridge.logger.severe("Command with content ${event.rawMessage} threw an exception.", throwable)
        when (event) {
            is DiscordMessageWrapper ->
                event.channel.sendMessage(
                        "${event.senderAsMention} | Ouch! That command threw an exception. Sorry about that... " +
                                "Have an admin check the logs for more info.").queue()
            is MinecraftChatEventWrapper ->
                event.player.sendMessage("${CC.ITALIC}${CC.GRAY}${DiscordBridge.getConfig(Cfg.CONFIG).getString("username", "DiscordBridge").stripColor()} whispers to you: " +
                        "Ouch! That command threw an exception. Sorry about that... " +
                        "Have an admin check the logs for more info.")
            is MinecraftCommandWrapper ->
                event.sender.sendMessage("${CC.ITALIC}${CC.GRAY}${DiscordBridge.getConfig(Cfg.CONFIG).getString("username", "DiscordBridge").stripColor()} whispers to you: " +
                        "Ouch! That command threw an exception. Sorry about that... " +
                        "Have an admin check the logs for more info.")
        }
    }

    // =============================================
    // ============== HELPER FUNCTIONS =============

    /**
     * Attempts to parse a string parameter to an expected value type
     *
     * @param parameterClass the expected type of the parameter
     * @param value a string representation of the value to be parsed
     */
    private fun parseArgument(parameterClass: Class<*>, value: String): Any {
        return try {
            when (parameterClass) {
                String::class.java -> value
                Int::class.javaPrimitiveType, Int::class.java -> Integer.valueOf(value)
                Long::class.javaPrimitiveType, Long::class.java -> java.lang.Long.valueOf(value)
                Boolean::class.javaPrimitiveType, Boolean::class.java -> parseBooleanArgument(value)
                Float::class.javaPrimitiveType, Float::class.java -> java.lang.Float.valueOf(value)
                Double::class.javaPrimitiveType, Double::class.java -> java.lang.Double.valueOf(value)
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
    private fun parseBooleanArgument(value: String): Boolean = when (value.toLowerCase()) {
        "yes", "true" -> true
        "no", "false" -> false
        else -> {
            val integerValue = Integer.valueOf(value)!!
            when (integerValue) {
                1 -> true
                0 -> false
                else -> throw IllegalArgumentException()
            }
        }
    }

    private inline fun <reified T : Any> List<HashMap<String, Any>>.castTo(factory: (HashMap<String, Any>) -> T): List<T> {
        return this.mapTo(mutableListOf()) { factory(it) }.toList()
    }
}
