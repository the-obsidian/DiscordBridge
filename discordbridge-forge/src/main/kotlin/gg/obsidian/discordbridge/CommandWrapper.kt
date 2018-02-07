package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.command.Command
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommand
import net.minecraft.command.ICommandSender
import net.minecraft.server.MinecraftServer

class CommandWrapper(private val command: Command): CommandBase() {
    override fun getUsage(sender: ICommandSender?): String {
        return "/$name ${command.usage}"
    }

    override fun getName(): String {
        return command.aliases[0]
    }

    override fun compareTo(other: ICommand?): Int {
        return if (other == null) 0 else super.compareTo(other)
    }

    override fun checkPermission(server: MinecraftServer?, sender: ICommandSender?): Boolean {
        // TODO
        return true
    }

    override fun getAliases(): MutableList<String> {
        return command.aliases.toMutableList()
    }

    override fun execute(server: MinecraftServer?, sender: ICommandSender?, args: Array<out String>?) {
        // Let the command listener handle this.
    }
}
