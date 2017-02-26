package gg.obsidian.discordbridge.minecraft.commands

import gg.obsidian.discordbridge.CommandLogic.askCleverbot
import gg.obsidian.discordbridge.Permissions
import gg.obsidian.discordbridge.Plugin
import gg.obsidian.discordbridge.Utils.*
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class Marina(val plugin: Plugin) : CommandExecutor {

    override fun onCommand(player: CommandSender, cmd: Command, alias: String?, args: Array<out String>?): Boolean {
        if (args == null || args.isEmpty()) return false

        if (player is Player && Permissions.cleverbot.has(player)) {
            val message: String = args.joinToString(" ")
            player.chat("@${plugin.cfg.USERNAME.noSpace()} $message")
            val response = askCleverbot(plugin.cfg.CLEVERBOT_KEY, message)
            plugin.sendToMinecraft(plugin.toMinecraftChatMessage(response, plugin.cfg.BOT_MC_USERNAME))
            plugin.sendToDiscord(response, plugin.connection.getRelayChannel())
            return true
        }
        return true
    }
}