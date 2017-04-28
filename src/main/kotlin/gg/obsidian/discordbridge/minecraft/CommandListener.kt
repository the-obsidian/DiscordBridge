package gg.obsidian.discordbridge.minecraft

import gg.obsidian.discordbridge.Plugin
import gg.obsidian.discordbridge.commands.MinecraftCommandWrapper
import gg.obsidian.discordbridge.commands.controllers.BotControllerManager
import gg.obsidian.discordbridge.commands.controllers.FunCommandsController
import gg.obsidian.discordbridge.commands.controllers.UtilCommandsController
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class CommandListener(val plugin: Plugin) : CommandExecutor {

    val controllerManager = BotControllerManager(plugin)

    init {
        controllerManager.registerController(FunCommandsController(plugin), minecraftExclusive = true)
        controllerManager.registerController(UtilCommandsController(plugin), minecraftExclusive = true)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return controllerManager.dispatchMessage(MinecraftCommandWrapper(sender, command, args))
    }

}