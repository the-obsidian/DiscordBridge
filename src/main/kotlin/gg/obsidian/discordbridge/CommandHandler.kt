package gg.obsidian.discordbridge

import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandHandler(val plugin: Plugin): CommandExecutor {

    override fun onCommand(player: CommandSender, cmd: Command, alias: String?, args: Array<out String>?): Boolean {
        if (cmd.name == "discord") return handleDiscord(player, args)
        else if (cmd.name == "registeralias") return handleRegisterAlias(player, args)
        else if (cmd.name == "getdiscordids") return handleGetDiscordIds(player)
        return false
    }

    private fun handleDiscord(player: CommandSender, args: Array<out String>?): Boolean {
        if (player is Player && !Permissions.reload.has(player)) return true

        val isConsole = (player is Player)

        if (args == null || args.size != 1 || args[0] != "reload") {
            sendMessage("&eUsage: /discord reload", player, isConsole)
            return true
        }

        sendMessage("&eReloading Discord Bridge...", player, isConsole)
        plugin.reload()
        return true
    }

    private fun handleRegisterAlias(player: CommandSender, args: Array<out String>?): Boolean {
        if (player !is Player) return true

        if (args == null || args.size != 1) {
            sendMessage("&eUsage: /registeralias <discord id>", player, false)
            return true
        }

        val users = plugin.getDiscordUsers()
        val found: Pair<String, String>? = users.find { it.second == args[0] }
        if (found == null) {
            sendMessage("&eCould not find Discord user with that ID.", player, false)
            return true
        }

        val ua: UserAlias = UserAlias(player.name, player.uniqueId.toString(), found.first,
                found.second)

        plugin.registerUserRequest(ua)
        return true
    }

    private fun handleGetDiscordIds(player: CommandSender): Boolean {
        val isConsole = (player !is Player)
        val users = plugin.getDiscordUsers()
        if (users.isEmpty()) {
            sendMessage("&eNo Discord members could be found. Either server is empty or an error " +
                    "has occurred.", player, isConsole)
            return true
        }
        val response = users.joinToString("\n- ", "&eDiscord users:\n- ")
        sendMessage(response, player, isConsole)
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
