package co.orre.discordbridge.commands.controllers

import co.orre.discordbridge.Config
import co.orre.discordbridge.Plugin
import co.orre.discordbridge.commands.*
import co.orre.discordbridge.commands.annotations.*
import co.orre.discordbridge.discord.Connection
import co.orre.discordbridge.utils.Script
import co.orre.discordbridge.utils.UtilFunctions.noSpace
import co.orre.discordbridge.utils.UtilFunctions.stripColor
import co.orre.discordbridge.utils.UtilFunctions.toDiscordChatMessage
import co.orre.discordbridge.utils.UtilFunctions.toMinecraftChatMessage
import java.lang.reflect.Method
import java.util.*
import java.util.logging.Level
import co.orre.discordbridge.Config as cfg
import org.bukkit.ChatColor as CC

class BotControllerManager(val plugin: Plugin) {

    private val commands: MutableMap<String, Command> = mutableMapOf()
    private val controllers: MutableMap<Class<out IBotController>, IBotController> = mutableMapOf()

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
                annotation.ignoreExcessArguments, isTagged, isPrivate, controllerClass, method)
        commands.put(command.name, command)
    }

    fun dispatchMessage(event: IEventWrapper): Boolean {
        plugin.logger.info("Entered dispatchMessage()")
        plugin.logger.info("Known commands: ${commands.keys}")

        // Short circuit if event was a Minecraft command
        if (event is MinecraftCommandWrapper) {
            plugin.logger.info("MinecraftCommandWrapper detected!")
            val command = commands[event.command.name]
            if (command == null) {
                commandNotFound(event, event.command.name)
                return true
            }
            plugin.logger.info("Command found: '${command.name}'")
            val inputArguments = event.rawMessage.split("\\s+".toRegex(), command.parameters.size).toTypedArray()
            return invokeCommand(command, controllers, event, inputArguments)
        }

        // Short circuit scripted responses
        if (scriptedResponse(event)) return true

        val args = event.rawMessage.trim().split("\\s+".toRegex(), 2).toTypedArray()

        // <prefix>command
        if (Config.COMMAND_PREFIX.isNotBlank() && args[0].startsWith(Config.COMMAND_PREFIX)) {
            plugin.logger.info("Prefixed command detected!")
            val commandName = args[0].substring(Config.COMMAND_PREFIX.length).toLowerCase()
            plugin.logger.info("Searching for command '$commandName'...")
            if (commandName == "") return true
            val command = commands[commandName]

            if (command == null) {
                commandNotFound(event, commandName)
                return false
            }
            plugin.logger.info("Command found: '${command.name}'")

            val inputArguments = if (args.size == 1) arrayOf<String>()
            else args[1].split("\\s+".toRegex(), command.parameters.size).toTypedArray()

            return invokeCommand(command, controllers, event, inputArguments)
        }

        // @<mention> command
        if ((event is AsyncPlayerChatEventWrapper && args[0] == "@"+Config.USERNAME.noSpace() ||
                args[0] == Connection.JDA.selfUser.asMention) && args.count() == 2) {
            plugin.logger.info("Bot Mention command detected!")
            val args2 = args[1].split("\\s+".toRegex(), 2).toTypedArray()
            val commandName = args2[0].toLowerCase()
            plugin.logger.info("Searching for command '$commandName'...")
            if (commandName == "") return true
            var params = if (args2.size > 1) args2[1] else ""
            var command = commands[commandName]

            if (command == null) {
                // Assume user wants to talk to Cleverbot
                command = commands["talk"]
                if (command == null) {
                    commandNotFound(event, commandName)
                    return false
                }
                params = args[1]
            }
            plugin.logger.info("Command found: '${command.name}'")

            val inputArguments = if (params == "") arrayOf<String>()
            else params.split("\\s+".toRegex(), command.parameters.size).toTypedArray()

            return invokeCommand(command, controllers, event, inputArguments)
        }

        // Just relay the message if it is neither
        relay(event, true)
        return true
    }

    private fun scriptedResponse(event: IEventWrapper): Boolean {
        val responses = plugin.script.data.getList("responses").checkItemsAre<Script>()
        if (responses == null) {
            plugin.logger.warning("ERROR: Unable to read script.yml!")
            return false
        }
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
        plugin.logger.info("A scripted response was found!")
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

    private fun invokeCommand(command: Command, instances: Map<Class<out IBotController>, IBotController>,
                              event: IEventWrapper, inputArguments: Array<String>): Boolean {
        plugin.logger.info("Entered invokeCommand()")
        // Relay the trigger if applicable
        if (command.relayTriggerMessage) relay(event, false)

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
                plugin.logger.info("Short-circuiting command 'help'")
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

            // Fail if not enough arguments
            inputArguments.size < command.parameters.size -> {
                commandWrongParameterCount(event, command.name, command.usage, inputArguments.size, command.parameters.size)
                return false
            }

            // Fail if command forces exact argument count AND the supplied argument count does not match expected
            inputArguments.size != command.parameters.size && !command.ignoreExcessArguments -> {
                commandWrongParameterCount(event, command.name, command.usage, inputArguments.size, command.parameters.size)
                return false
            }
        }

        // Package arguments to send to method
        val arguments = arrayOfNulls<Any>(command.parameters.size + 1)
        arguments[0] = event
        val paramRange = if (command.ignoreExcessArguments && inputArguments.size > command.parameters.size)
            IntRange(0, command.parameters.size - 1) else command.parameters.indices
        for (i in paramRange) {
            val parameterClass = command.parameters[i]

            try {
                arguments[i + 1] = parseArgument(parameterClass, inputArguments[i])
            } catch (ignored: IllegalArgumentException) {
                commandWrongParameterType(event, command.name, command.usage, i, inputArguments[i].javaClass, parameterClass)
                return false
            }
        }

        // Invoke the method
        plugin.logger.info("Attempting to invoke command...")
        try {
            val response = command.commandMethod.invoke(instances[command.controllerClass], *arguments) as? String ?: return true
            respond(event, command, response)
            return true
        } catch (e: IllegalArgumentException) {
            commandWrongParameterCount(event, command.name, command.usage, inputArguments.size, command.parameters.size)
            return false
        } catch (e: Exception) {
            commandException(event, e)
            return true
        }

    }

    private fun respond(event: IEventWrapper, command: Command, response: String) {
        var modifiedResponse = response
        when (event) {
            is AsyncPlayerChatEventWrapper -> {
                if (command.isPrivate) {
                    modifiedResponse = "${CC.ITALIC}${CC.GRAY}${Config.BOT_MC_USERNAME.stripColor()} whispers to you: " +
                            modifiedResponse
                    event.event.player.sendMessage(modifiedResponse)
                    return
                }
                if (command.isTagged) modifiedResponse = "${event.senderAsMention} | $modifiedResponse"
                plugin.sendToMinecraft(modifiedResponse.toMinecraftChatMessage(Config.BOT_MC_USERNAME))

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
                plugin.sendToMinecraft(modifiedResponse)
                return
            }
            is MinecraftCommandWrapper -> {
                if (command.isPrivate || command.isTagged) {
                    modifiedResponse = "${CC.ITALIC}${CC.GRAY}${Config.BOT_MC_USERNAME.stripColor()} whispers to you: " +
                            modifiedResponse
                    event.sender.sendMessage(modifiedResponse)
                    return
                }
                plugin.sendToMinecraft(modifiedResponse.toMinecraftChatMessage(Config.BOT_MC_USERNAME))

                modifiedResponse = plugin.convertAtMentions(modifiedResponse)
                modifiedResponse = plugin.translateAliasesToDiscord(modifiedResponse)
                plugin.sendToDiscord(modifiedResponse, event.channel)
                return
            }
        }
    }

    private fun relay(event: IEventWrapper, logIgnore: Boolean) {
        plugin.logger.info("Entered relay()")
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
                    plugin.sendToMinecraft(formattedMessage)
                } else if (logIgnore) plugin.logDebug("Not relaying message from Discord: channel does not match")
            }
            else -> return // slash commands do not get relayed
        }
    }

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

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Any> List<*>.checkItemsAre() = if (all { it is T }) this as List<T> else null

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

    private fun orPrefixHelp(): String = if (Config.COMMAND_PREFIX != "") "or ${Config.COMMAND_PREFIX}help " else ""

    data class Command(
            val name: String,
            val usage: String,
            val description: String,
            val parameters: List<Class<*>>,
            val relayTriggerMessage: Boolean,
            val ignoreExcessArguments: Boolean,
            val isTagged: Boolean,
            val isPrivate: Boolean,
            val controllerClass: Class<*>,
            val commandMethod: Method
    )
}
