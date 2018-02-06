package gg.obsidian.discordbridge.commands

import java.lang.reflect.Method

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
        val aliases: Array<String>,
        val usage: String,
        val description: String,
        val help: String,
        val parameters: List<Class<*>>,
        val relayTriggerMessage: Boolean,
        val squishExcessArgs: Boolean,
        val ignoreExcessArgs: Boolean,
        val isTagged: Boolean,
        val isPrivate: Boolean,
        val controllerClass: Class<*>,
        val commandMethod: Method
)