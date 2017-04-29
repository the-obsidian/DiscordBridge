package gg.obsidian.discordbridge.commands

import gg.obsidian.discordbridge.discord.Connection
import net.dv8tion.jda.core.entities.MessageChannel
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * A wrapper for the parameters passed to onCommand() in Bukkit's CommandExecutor class
 *
 * @param sender the sender of the command
 * @param command the command that was invoked
 * @param args an array of argument strings passed to the command
 */
class MinecraftCommandWrapper(val sender: CommandSender, val command: Command,
                              val args: Array<out String>) : IEventWrapper {
    /**
     * The Minecraft username of the command sender
     *
     * Returns "Console" if the command was sent from the server console
     */
    override val senderName: String
        get() = if (sender is Player) sender.name else "Console"
    /**
     * Returns a space-delimited string of all the arguments passed with the command
     *
     * This is identical to rawMessage
     */
    override val message: String
        get() = args.joinToString(separator = " ")
    /**
     * Returns a space-delimited string of all the arguments passed with the command
     *
     * This is identical to message
     */
    override val rawMessage: String
        get() = args.joinToString(separator = " ")
    /**
     * The Minecraft username of the command sender in "@name" format
     */
    override val senderAsMention: String
        get() = "@${sender.name}"
    /**
     * Returns the value at Connection.getRelayChannel()
     * @see Connection.getRelayChannel
     */
    override val channel: MessageChannel
        get() = Connection.getRelayChannel()!!
    /**
     * The command sender's Minecraft UUID
     */
    override val senderId: String
        get() = (sender as? Player)?.uniqueId?.toString() ?: ""
    /**
     * Always returns true for this wrapper type
     */
    override val isFromRelayChannel: Boolean
        get() = true

}
