package gg.obsidian.discordbridge.commands

import gg.obsidian.discordbridge.discord.Connection
import net.dv8tion.jda.core.entities.MessageChannel
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class MinecraftCommandWrapper(val sender: CommandSender, val command: Command,
                              val args: Array<out String>) : IEventWrapper {
    override val senderName: String
        get() = if (sender is Player) sender.name else "Console"
    override val message: String
        get() = args.joinToString(separator = " ")
    override val rawMessage: String
        get() = args.joinToString(separator = " ")
    override val senderAsMention: String
        get() = "@${sender.name}"
    override val channel: MessageChannel
        get() = Connection.getRelayChannel()!!
    override val senderId: String
        get() = (sender as? Player)?.uniqueId?.toString() ?: ""
    override val isFromRelayChannel: Boolean
        get() = true

}
