package gg.obsidian.discordbridge.wrappers

import gg.obsidian.discordbridge.commands.controllers.BotControllerManager
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommand
import net.minecraft.command.ICommandSender
import net.minecraft.server.MinecraftServer

class CommandWrapper(private val command: BotControllerManager.Command): CommandBase() {
    override fun getUsage(sender: ICommandSender?): String {
        return "/${command.name} ${command.usage}"
    }

    override fun getName(): String {
        return command.name
    }

    override fun compareTo(other: ICommand?): Int {
        return if (other == null) 0 else super.compareTo(other)
    }

    override fun checkPermission(server: MinecraftServer?, sender: ICommandSender?): Boolean {
        return true
    }

    override fun getAliases(): MutableList<String> {
        // TODO: Add support for command aliases
        return mutableListOf()
    }

    override fun execute(server: MinecraftServer?, sender: ICommandSender?, args: Array<out String>?) {
        // Let the command listener handle this.
    }

}