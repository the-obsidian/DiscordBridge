package co.orre.discordbridge.minecraft.commands

import co.orre.discordbridge.Plugin
import co.orre.discordbridge.commands.MinecraftCommandWrapper
import co.orre.discordbridge.commands.controllers.BotControllerManager
import co.orre.discordbridge.commands.controllers.FunCommandsController
import co.orre.discordbridge.commands.controllers.UtilCommandsController
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