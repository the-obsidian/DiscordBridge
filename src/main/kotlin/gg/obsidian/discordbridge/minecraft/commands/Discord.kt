package gg.obsidian.discordbridge.minecraft.commands

import gg.obsidian.discordbridge.minecraft.Permissions
import gg.obsidian.discordbridge.Plugin
import org.bukkit.ChatColor as CC
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class Discord(val plugin: Plugin) : CommandExecutor {

    override fun onCommand(player: CommandSender, cmd: Command, alias: String?, args: Array<out String>?): Boolean {
        if (args == null || args.isEmpty()) {
            sendMessage("{${CC.YELLOW}Usage: /discord <reload/alias/get>", player)
            return true
        }

        when (args[0].toLowerCase()) {
            "reload" -> return handleDiscordReload(player, args)
            "alias" -> return handleDiscordAlias(player, args)
            "get" -> return handleDiscordGet(player, args)
            else -> {
                sendMessage("${CC.YELLOW}Usage: /discord <reload/alias/get>", player)
                return true
            }
        }
    }

    private fun handleDiscordReload(player: CommandSender, args: Array<out String>): Boolean {
        if (player is Player && !Permissions.reload.has(player)) return true

        if (args.size != 1) {
            sendMessage("${CC.YELLOW}Usage: /discord reload", player)
            return true
        }

        sendMessage("${CC.YELLOW}Reloading Discord Bridge...", player)
        plugin.reload()
        return true
    }

    private fun handleDiscordAlias(player: CommandSender, args: Array<out String>): Boolean {
        if (player !is Player) return true

        if (args.size != 2) {
            sendMessage("${CC.YELLOW}Usage: /discord alias <discord id>", player)
            return true
        }

        if (!plugin.registerUserRequest(player, args[1])) {
            sendMessage("${CC.YELLOW}Could not find Discord user with that ID.", player)
            return true
        }
        return true
    }

    private fun handleDiscordGet(player: CommandSender, args: Array<out String>): Boolean {
        if (args.size < 2) {
            sendMessage("${CC.YELLOW}Usage: /discord get <ids/online>", player)
            return true
        }

        when (args[1].toLowerCase()) {
            "ids" -> return handleDiscordGetIds(player, args)
            "online" -> return handleDiscordGetOnline(player, args)
            else -> {
                sendMessage("${CC.YELLOW}Usage: /discord get <ids/online>", player)
                return true
            }
        }
    }

    private fun handleDiscordGetIds(player: CommandSender, args: Array<out String>): Boolean {
        if (args.size != 2) {
            sendMessage("${CC.YELLOW}Usage: /discord get ids", player)
            return true
        }

        sendMessage(plugin.getDiscordIds(), player)
        return true
    }

    private fun handleDiscordGetOnline(player: CommandSender, args: Array<out String>): Boolean {
        if (args.size != 2) {
            sendMessage("${CC.YELLOW}Usage: /discord get online", player)
            return true
        }

        sendMessage(plugin.getDiscordOnline(), player)
        return true
    }

    private fun sendMessage(message: String, player: CommandSender) {
        if (player is Player)
            player.sendMessage(message)
        else
            plugin.server.consoleSender.sendMessage(message)
    }
}
