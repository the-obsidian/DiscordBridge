package co.orre.discordbridge.discord.controllers

// TODO: Add Apache license

import co.orre.discordbridge.Config
import co.orre.discordbridge.Plugin
import co.orre.discordbridge.discord.Connection
import co.orre.discordbridge.discord.interfaces.BotCommand
import co.orre.discordbridge.discord.interfaces.BotController
import co.orre.discordbridge.utils.Script
import co.orre.discordbridge.utils.UtilFunctions.noSpace
import co.orre.discordbridge.utils.UtilFunctions.toMinecraftChatMessage
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Message
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*
import java.util.logging.Level
import co.orre.discordbridge.Config as cfg

class BotControllerManager(val plugin: Plugin) {

    private val commands: MutableMap<String, Command> = mutableMapOf()
    private val controllers: MutableMap<Class<out BotController>, BotController> = mutableMapOf()

    fun registerController(controller: BotController) {
        controllers.put(controller.javaClass, controller)
        val controllerClass = controller.javaClass

        for (method in controllerClass.declaredMethods) {
            val annotation = method.getAnnotation(BotCommand::class.java)
            if (annotation != null) registerControllerMethod(controllerClass, method, annotation)
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

    fun dispatchMessage(message: Message) {

        // SCRIPTED RESPONSE - The bot replies with a pre-programmed response if it detects
        // a corresponding trigger string
        if (scriptedResponse(message)) return

        val args = message.rawContent.trim().split("\\s+".toRegex(), 2).toTypedArray()

        // <prefix>command
        if (Config.COMMAND_PREFIX.isNotBlank() && args[0].startsWith(Config.COMMAND_PREFIX)) {
            val commandName = args[0].substring(Config.COMMAND_PREFIX.length).toLowerCase()
            val command = commands[commandName]

            if (command == null) {
                commandNotFound(message, commandName)
                return
            }

            val inputArguments = if (args.size == 1) arrayOf<String>()
                else args[1].split("\\s+".toRegex(), command.parameters.size).toTypedArray()

            invokeCommand(command, controllers, message, inputArguments)
            return
        }

        // @<mention> command
        if (args[0] == Connection.JDA.selfUser.asMention && args.count() == 2) {
            val args2 = args[1].split("\\s+".toRegex(), 2).toTypedArray()
            val commandName = args2[0].toLowerCase()
            var params = if (args2.size > 1) args2[1] else ""
            var command = commands[commandName]

            if (command == null) {
                // Assume user wants to talk to Cleverbot
                command = commands["talk"]
                if (command == null) {
                    commandNotFound(message, commandName)
                    return
                }
                params = args[1]
            }

            val inputArguments = if (params == "") arrayOf<String>()
            else params.split("\\s+".toRegex(), command.parameters.size).toTypedArray()

            invokeCommand(command, controllers, message, inputArguments)
            return
        }

        // Just relay the message if it is neither
        relay(message, true)
    }

    private fun scriptedResponse(message: Message): Boolean {
        val responses = plugin.script.data.getList("responses").checkItemsAre<Script>()
        if (responses == null) {
            plugin.logger.warning("ERROR: Unable to read script.yml!")
            return false
        }
        var response: Script? = null
        for (r in responses) {
            var arg = message.rawContent
            val msg = r.triggerDis ?: continue
            val ignorecase = !(r.caseSensitive != null && r.caseSensitive)
            val startswith = r.startsWith != null && r.startsWith
            val requiresmention = r.requiresMention != null && r.requiresMention

            if (requiresmention) {
                if (arg.startsWith(Connection.JDA.selfUser.asMention, true))
                    arg = arg.replaceFirst(Connection.JDA.selfUser.asMention, "").trimStart()
                else continue
            }

            if (startswith)
                if (arg.startsWith(msg, ignorecase)) response = r
                else
                    if (arg.equals(msg, ignorecase)) response = r
        }
        if (response == null) return false
        plugin.logDebug("user ${message.author.name} has triggered a scripted response")
        val responseDis: String? = response.responseDis
        val responseMC: String? = response.responseMC
        if (responseDis != null) {
            val out = responseDis.replace("%u", message.author.name)
            plugin.sendToDiscord(out, message.channel)
        }
        if (responseMC != null && message.isFromRelayChannel()) {
            var alias = plugin.users.data.getString("discordaliases.${message.author.id}.mcusername")
            if (alias == null) alias = message.author.name.noSpace()
            val out = responseMC.replace("%u", alias)
            plugin.sendToMinecraft(out.toMinecraftChatMessage(cfg.BOT_MC_USERNAME))
        }
        return true
    }

    private fun invokeCommand(command: Command, instances: Map<Class<out BotController>, BotController>,
                              message: Message, inputArguments: Array<String>) {
        if (command.relayTriggerMessage) relay(message, false)

        if (command.name == "help") {
            try {
                command.commandMethod.invoke(instances[command.controllerClass], message, commands, instances)
                return
            } catch (e: InvocationTargetException) {
                commandException(message, e.cause)
                return
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            } catch (e: IllegalArgumentException) {
                commandWrongParameterCount(message, command.name, command.usage, 2, command.parameters.size)
                return
            }
        }

        if (inputArguments.size < command.parameters.size) {
            commandWrongParameterCount(message, command.name, command.usage, inputArguments.size, command.parameters.size)
            return
        }
        if (inputArguments.size != command.parameters.size && !command.ignoreExcessArguments) {
            commandWrongParameterCount(message, command.name, command.usage, inputArguments.size, command.parameters.size)
            return
        }

        val arguments = arrayOfNulls<Any>(command.parameters.size + 1)
        arguments[0] = message

        var paramRange = command.parameters.indices

        if (command.ignoreExcessArguments && inputArguments.size > command.parameters.size)
            paramRange = IntRange(0, command.parameters.size - 1)

        for (i in paramRange) {
            val parameterClass = command.parameters[i]

            try {
                arguments[i + 1] = parseArgument(parameterClass, inputArguments[i])
            } catch (ignored: IllegalArgumentException) {
                commandWrongParameterType(message, command.name, command.usage, i,
                        inputArguments[i].javaClass, parameterClass)
                return
            }
        }

        try {
            command.commandMethod.invoke(instances[command.controllerClass], *arguments)
        } catch (e: InvocationTargetException) {
            commandException(message, e.cause)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        } catch (e: IllegalArgumentException) {
            commandWrongParameterCount(message, command.name, command.usage, inputArguments.size, command.parameters.size)
            return
        }
    }

    private fun relay(message: Message, logIgnore: Boolean) {
        if (message.isFromRelayChannel()) {
            plugin.logDebug("Broadcasting message ${message.id} from Discord to Minecraft as user ${message.author.name}")
            var alias = plugin.users.data.getString("discordaliases.${message.author.id}.mcusername")
            if (alias == null) alias = message.author.name.noSpace()
            plugin.sendToMinecraft(message.content.toMinecraftChatMessage(alias))
        } else if (logIgnore) plugin.logDebug("Not relaying message ${message.id} from Discord: channel does not match")
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

    private fun commandNotFound(message: Message, commandName: String) {
        message.channel.sendMessage(
                "${message.author.asMention} | I don't seem to have a command called '$commandName'. " +
                "See '${Connection.JDA.selfUser.asMention} help' ${orPrefixHelp()}for the commands I do have.").queue()
    }

    private fun commandWrongParameterCount(message: Message, name: String, usage: String, given: Int, required: Int) {
        message.channel.sendMessage(
                "${message.author.asMention} | I didn't seem to get the correct number of arguments for that command. " +
                "I got $given from you, but I need $required. " +
                "(Usage: $name $usage)").queue()
    }

    private fun commandWrongParameterType(message: Message, name: String, usage: String, index: Int,
                                          actualType: Class<*>, expectedType: Class<*>) {
        message.channel.sendMessage(
                "${message.author.asMention} | One of the arguments for that command doesn't seem to be the right type. " +
                "Argument $index seems like ${actualType.simpleName}, but I need ${expectedType.simpleName}. " +
                "(Usage: $name $usage)").queue()
    }

    @Suppress("unused")
    private fun commandRestricted(message: Message) {
        message.channel.sendMessage("${message.author.asMention} | Sorry, that command is not permitted.").queue()
    }

    private fun commandException(message: Message, throwable: Throwable?) {
        plugin.logger.log(Level.SEVERE, "Command with content ${message.content} threw an exception.", throwable)
        message.channel.sendMessage("${message.author.asMention} | Ouch! That command threw an exception. " +
                "Sorry about that...").queue()
    }

    private fun orPrefixHelp(): String = if (cfg.COMMAND_PREFIX != "") "or ${cfg.COMMAND_PREFIX}help " else ""

    private fun Message.isFromRelayChannel():
            Boolean = guild.id == cfg.SERVER_ID
            && this.isFromType(ChannelType.TEXT)
            && this.textChannel.name.equals(cfg.CHANNEL, true)

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
