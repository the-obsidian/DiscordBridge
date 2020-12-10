package gg.obsidian.discordbridge.command

import java.lang.reflect.Method

/**
 * Represents a command that the bot can execute
 *
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
) {
        override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Command

                if (!aliases.contentEquals(other.aliases)) return false
                if (usage != other.usage) return false
                if (description != other.description) return false
                if (help != other.help) return false
                if (parameters != other.parameters) return false
                if (relayTriggerMessage != other.relayTriggerMessage) return false
                if (squishExcessArgs != other.squishExcessArgs) return false
                if (ignoreExcessArgs != other.ignoreExcessArgs) return false
                if (isTagged != other.isTagged) return false
                if (isPrivate != other.isPrivate) return false
                if (controllerClass != other.controllerClass) return false
                if (commandMethod != other.commandMethod) return false

                return true
        }

        override fun hashCode(): Int {
                var result = aliases.contentHashCode()
                result = 31 * result + usage.hashCode()
                result = 31 * result + description.hashCode()
                result = 31 * result + help.hashCode()
                result = 31 * result + parameters.hashCode()
                result = 31 * result + relayTriggerMessage.hashCode()
                result = 31 * result + squishExcessArgs.hashCode()
                result = 31 * result + ignoreExcessArgs.hashCode()
                result = 31 * result + isTagged.hashCode()
                result = 31 * result + isPrivate.hashCode()
                result = 31 * result + controllerClass.hashCode()
                result = 31 * result + commandMethod.hashCode()
                return result
        }
}
