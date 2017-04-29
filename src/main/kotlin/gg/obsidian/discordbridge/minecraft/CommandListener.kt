package gg.obsidian.discordbridge.minecraft

import gg.obsidian.discordbridge.Plugin
import gg.obsidian.discordbridge.commands.MinecraftCommandWrapper
import gg.obsidian.discordbridge.commands.controllers.BotControllerManager
import gg.obsidian.discordbridge.commands.controllers.FunCommandsController
import gg.obsidian.discordbridge.commands.controllers.UtilCommandsController
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

/**
 * Listens for commands sent to the Minecraft server
 *
 * @param plugin a reference to the base Plugin object
 */
class CommandListener(val plugin: Plugin) : CommandExecutor {

    val controllerManager = BotControllerManager(plugin)

    init {
        controllerManager.registerController(FunCommandsController(plugin), minecraftExclusive = true)
        controllerManager.registerController(UtilCommandsController(plugin), minecraftExclusive = true)
    }

    /**
     * Callback for a captured command event
     *
     * @param sender the sender of the command
     * @param command the command that was captured
     * @param label no idea lol
     * @param args an array of argument strings passed to the command
     * @return whether the command "succeeded". this returns false, the default usage message will be sent to the sender
     */
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return controllerManager.dispatchMessage(MinecraftCommandWrapper(sender, command, args))
    }

}