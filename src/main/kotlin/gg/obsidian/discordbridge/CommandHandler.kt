package gg.obsidian.discordbridge

import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandHandler(val plugin: Plugin): CommandExecutor {

    override fun onCommand(player: CommandSender, cmd: Command, alias: String?, args: Array<out String>?): Boolean {
        if (player is Player && !Permissions.reload.has(player)) return true

        val isConsole = (player is Player)

        if (cmd.name != "discord") return true

        if (args == null || args.size != 1 || !args[0].equals("reload")) {
            sendMessage("&eUsage: /discord reload", player, isConsole)
            return true
        }

        sendMessage("&eReloading Discord Bridge...", player, isConsole)
        plugin.reload()
        return true
    }

    private fun sendMessage(message: String, player: CommandSender, isConsole: Boolean) {
        val formattedMessage = ChatColor.translateAlternateColorCodes('&', message)
        if (isConsole) {
            plugin.server.consoleSender.sendMessage(formattedMessage)
        } else {
            player.sendMessage(formattedMessage)
        }
    }
}
