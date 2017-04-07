package co.orre.discordbridge.commands.controllers

// TODO: Add Apache license

import co.orre.discordbridge.Config
import co.orre.discordbridge.Plugin
import co.orre.discordbridge.commands.*
import co.orre.discordbridge.commands.Annotations.BotCommand
import co.orre.discordbridge.commands.Annotations.ChatExclusiveCommand
import co.orre.discordbridge.commands.Annotations.DiscordExclusiveCommand
import co.orre.discordbridge.commands.Annotations.MinecraftExclusiveCommand
import co.orre.discordbridge.discord.Connection
import co.orre.discordbridge.utils.Script
import co.orre.discordbridge.utils.UtilFunctions.stripColor
import co.orre.discordbridge.utils.UtilFunctions.toMinecraftChatMessage
import net.dv8tion.jda.core.entities.Message
import org.bukkit.ChatColor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*
import java.util.logging.Level
import co.orre.discordbridge.Config as cfg

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

        if (methodParameters.isEmpty() || !methodParameters[0].type.isAssignableFrom(Message::class.java)) return

        method.isAccessible = true
        val parameters = (1..methodParameters.size - 1).mapTo(ArrayList<Class<*>>()) { methodParameters[it].type }
        val command = Command(commandName, usage, annotation.description, parameters, annotation.relayTriggerMessage,
                annotation.ignoreExcessArguments, controllerClass, method)
        commands.put(command.name, command)
    }

    fun dispatchMessage(event: IEventWrapper): Boolean {

        if (event is MinecraftCommandWrapper) {
            val command = commands[event.cmd.name] ?: return false
            val inputArguments = event.rawMessage.split("\\s+".toRegex(), command.parameters.size).toTypedArray()
            return invokeCommand(command, controllers, event, inputArguments)
        }

        // SCRIPTED RESPONSE - The bot replies with a pre-programmed response if it detects
        // a corresponding trigger string
        if (scriptedResponse(event)) return true

        val args = event.rawMessage.trim().split("\\s+".toRegex(), 2).toTypedArray()

        // <prefix>command
        if (Config.COMMAND_PREFIX.isNotBlank() && args[0].startsWith(Config.COMMAND_PREFIX)) {
            val commandName = args[0].substring(Config.COMMAND_PREFIX.length).toLowerCase()
            if (commandName == "") return true
            val command = commands[commandName]

            if (command == null) {
                commandNotFound(event, commandName)
                return false
            }

            val inputArguments = if (args.size == 1) arrayOf<String>()
                else args[1].split("\\s+".toRegex(), command.parameters.size).toTypedArray()

            return invokeCommand(command, controllers, event, inputArguments)
        }

        // @<mention> command
        if (args[0] == Connection.JDA.selfUser.asMention && args.count() == 2) {
            val args2 = args[1].split("\\s+".toRegex(), 2).toTypedArray()
            val commandName = args2[0].toLowerCase()
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
        // TODO: Switch if Minecraft or Discord
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

            when (event.type) {
                WrapperType.ASYNC_PLAYER_CHAT_EVENT -> {
                    if (startswith)
                        if (triggerMC.isNotEmpty() && arg.startsWith(triggerMC, ignorecase)) response = r
                        else
                            if (triggerMC.isNotEmpty() && arg.equals(triggerMC, ignorecase)) response = r
                }
                WrapperType.MESSAGE -> {
                    if (startswith)
                        if (triggerDis.isNotEmpty() && arg.startsWith(triggerDis, ignorecase)) response = r
                        else
                            if (triggerDis.isNotEmpty() && arg.equals(triggerDis, ignorecase)) response = r
                }
                else -> return false
            }
        }
        if (response == null) return false
        plugin.logDebug("user ${event.senderName} has triggered a scripted response")
        val responseDis: String? = response.responseDis
        val responseMC: String? = response.responseMC
        if (responseDis != null) {
            val out = responseDis.replace("%u", event.senderAsMention)
            plugin.sendToDiscord(out, event.channel)
        }
        if (responseMC != null && event.isFromRelayChannel) {
            var alias = plugin.users.data.getString("discordaliases.${message.author.id}.mcusername")
            if (alias == null) alias = message.author.name.noSpace()
            val out = responseMC.replace("%u", alias)
            plugin.sendToMinecraft(out.toMinecraftChatMessage(Config.BOT_MC_USERNAME))
        }
        return true
    }

    private fun invokeCommand(command: Command, instances: Map<Class<out IBotController>, IBotController>,
                              event: IEventWrapper, inputArguments: Array<String>): Boolean {
        if (command.relayTriggerMessage) relay(event, false)

        if (command.name == "help") {
            try {
                command.commandMethod.invoke(instances[command.controllerClass], event, commands, instances)
                return true
            } catch (e: InvocationTargetException) {
                commandException(event, e.cause)
                return false
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            } catch (e: IllegalArgumentException) {
                commandWrongParameterCount(event, command.name, command.usage, 2, command.parameters.size)
                return false
            }
        }

        if (inputArguments.size < command.parameters.size) {
            commandWrongParameterCount(event, command.name, command.usage, inputArguments.size, command.parameters.size)
            return false
        }
        if (inputArguments.size != command.parameters.size && !command.ignoreExcessArguments) {
            commandWrongParameterCount(event, command.name, command.usage, inputArguments.size, command.parameters.size)
            return false
        }

        val arguments = arrayOfNulls<Any>(command.parameters.size + 1)
        arguments[0] = event

        var paramRange = command.parameters.indices

        if (command.ignoreExcessArguments && inputArguments.size > command.parameters.size)
            paramRange = IntRange(0, command.parameters.size - 1)

        for (i in paramRange) {
            val parameterClass = command.parameters[i]

            try {
                arguments[i + 1] = parseArgument(parameterClass, inputArguments[i])
            } catch (ignored: IllegalArgumentException) {
                commandWrongParameterType(event, command.name, command.usage, i,
                        inputArguments[i].javaClass, parameterClass)
                return false
            }
        }

        try {
            command.commandMethod.invoke(instances[command.controllerClass], *arguments)
            return true
        } catch (e: InvocationTargetException) {
            commandException(event, e.cause)
            return false
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        } catch (e: IllegalArgumentException) {
            commandWrongParameterCount(event, command.name, command.usage, inputArguments.size, command.parameters.size)
            return false
        }
    }

    private fun relay(event: IEventWrapper, logIgnore: Boolean) {
        when (event.type) {
            WrapperType.ASYNC_PLAYER_CHAT_EVENT -> {

            }
            WrapperType.MESSAGE -> {
                if (event.isFromRelayChannel) {
                    plugin.logDebug("Broadcasting message from Discord to Minecraft as user ${event.senderName}")
                    //var alias = plugin.users.data.getString("discordaliases.${message.author.id}.mcusername")
                    //if (alias == null) alias = message.author.name.noSpace()
                    plugin.sendToMinecraft(message.content.toMinecraftChatMessage(alias))
                } else if (logIgnore) plugin.logDebug("Not relaying message ${message.id} from Discord: channel does not match")
            }
            else -> return
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
                    event.event.player.sendMessage("${ChatColor.ITALIC}${Config.BOT_MC_USERNAME.stripColor()} whispers to you: " +
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
                event.event.player.sendMessage("${ChatColor.ITALIC}${Config.BOT_MC_USERNAME.stripColor()} whispers to you: " +
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
                event.event.player.sendMessage("${ChatColor.ITALIC}${Config.BOT_MC_USERNAME.stripColor()} whispers to you: " +
                        "One of the arguments for that command doesn't seem to be the right type. " +
                        "Argument $index seems like ${actualType.simpleName}, but I need ${expectedType.simpleName}. " +
                        "(Usage: $name $usage)")
        }
    }

    @Suppress("unused")
    private fun commandRestricted(event: IEventWrapper) {
        when (event) {
            is MessageWrapper ->
                event.channel.sendMessage("${event.senderAsMention} | Sorry, that command is not permitted.").queue()
            is AsyncPlayerChatEventWrapper ->
                event.event.player.sendMessage("${ChatColor.ITALIC}${Config.BOT_MC_USERNAME.stripColor()} whispers to you: " +
                        "Sorry, that command is not permitted.")
        }
    }

    private fun commandException(event: IEventWrapper, throwable: Throwable?) {
        plugin.logger.log(Level.SEVERE, "Command with content ${event.rawMessage} threw an exception.", throwable)
        when (event) {
            is MessageWrapper ->
                event.channel.sendMessage(
                        "${event.senderAsMention} | Ouch! That command threw an exception. Sorry about that...").queue()
            is AsyncPlayerChatEventWrapper ->
                event.event.player.sendMessage("${ChatColor.ITALIC}${Config.BOT_MC_USERNAME.stripColor()} whispers to you: " +
                        "Ouch! That command threw an exception. Sorry about that...")
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
            val controllerClass: Class<*>,
            val commandMethod: Method
    )
}
