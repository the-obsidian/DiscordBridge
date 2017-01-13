package gg.obsidian.discordbridge

import net.dv8tion.jda.core.OnlineStatus
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandHandler(val plugin: Plugin): CommandExecutor {

    override fun onCommand(player: CommandSender, cmd: Command, alias: String?, args: Array<out String>?): Boolean {
        if (cmd.name == "discord") return handleDiscord(player, args)
        return false
    }

    private fun handleDiscord(player: CommandSender, args: Array<out String>?): Boolean {
        if (args == null || args.isEmpty()) {
            sendMessage("&eUsage: /discord <reload/alias/get>", player)
            return true
        }

        when (args[0].toLowerCase()) {
            "reload" -> return handleDiscordReload(player, args)
            "alias" -> return handleDiscordAlias(player, args)
            "get" -> return handleDiscordGet(player, args)
            else -> {
                sendMessage("&eUsage: /discord <reload/alias/get>", player)
                return true
            }
        }
    }

    private fun handleDiscordReload(player: CommandSender, args: Array<out String>): Boolean {
        if (player is Player && !Permissions.reload.has(player)) return true

        if (args.size != 1) {
            sendMessage("&eUsage: /discord reload", player)
            return true
        }

        sendMessage("&eReloading Discord Bridge...", player)
        plugin.reload()
        return true
    }

    private fun handleDiscordAlias(player: CommandSender, args: Array<out String>): Boolean {
        if (player !is Player) return true

        if (args.size != 2) {
            sendMessage("&eUsage: /discord alias <discord id>", player)
            return true
        }

        val users = plugin.getDiscordUsers()
        val found: Triple<String, String, Boolean>? = users.find { it.second == args[0] }
        if (found == null) {
            sendMessage("&eCould not find Discord user with that ID.", player)
            return true
        }

        val ua: UserAlias = UserAlias(player.name, player.uniqueId.toString(), found.first,
                found.second)

        plugin.registerUserRequest(ua)
        return true
    }

    private fun handleDiscordGet(player: CommandSender, args: Array<out String>): Boolean {
        if (args.size < 2) {
            sendMessage("&eUsage: /discord get <ids/online>", player)
            return true
        }

        when (args[1].toLowerCase()) {
            "ids" -> return handleDiscordGetIds(player, args)
            "online" -> return handleDiscordGetOnline(player, args)
            else -> {
                sendMessage("&eUsage: /discord get <ids/online>", player)
                return true
            }
        }
    }

    private fun handleDiscordGetIds(player: CommandSender, args: Array<out String>): Boolean {
        if (args.size != 2) {
            sendMessage("&eUsage: /discord get ids", player)
            return true
        }

        val users = plugin.getDiscordUsers()

        if (users.isEmpty()) {
            sendMessage("&eNo Discord members could be found. Either server is empty or an error " +
                    "has occurred.", player)
            return true
        }

        var response = "&eDiscord users:"
        for (user in users) {
            if (user.third) response += "\n&6- ${user.first} (Bot), ${user.second}&r"
            else response += "\n&e- ${user.first}, ${user.second}&r"
        }
        //val response = users.joinToString("\n- ", "&eDiscord users:\n- ")
        sendMessage(response, player)
        return true
    }

    private fun handleDiscordGetOnline(player: CommandSender, args: Array<out String>): Boolean {
        if (args.size != 2) {
            sendMessage("&eUsage: /discord get online", player)
            return true
        }

        val users = plugin.getDiscordOnline()

        if (users.isEmpty()) {
            sendMessage("&eNo Discord members could be found. Either server is empty or an error " +
                    "has occurred.", player)
            return true
        }

        var response = ""
        if (users.filter{it.third == OnlineStatus.ONLINE}.isNotEmpty()) {
            response += "\n&2Online:&r"
            for (user in users.filter{it.third == OnlineStatus.ONLINE}) {
                if (user.second) response += "\n&2- ${user.first} (Bot)&r"
                else response += "\n&2- ${user.first}&r"
            }
        }
        if (users.filter{it.third == OnlineStatus.IDLE}.isNotEmpty()) {
            response += "\n&eIdle:&r"
            for (user in users.filter{it.third == OnlineStatus.IDLE}) {
                if (user.second) response += "\n&e- ${user.first} (Bot)&r"
                else response += "\n&e- ${user.first}&r"
            }
        }
        if (users.filter{it.third == OnlineStatus.DO_NOT_DISTURB}.isNotEmpty()) {
            response += "\n&cDo Not Disturb:&r"
            for (user in users.filter { it.third == OnlineStatus.DO_NOT_DISTURB }) {
                if (user.second) response += "\n&c- ${user.first} (Bot)&r"
                else response += "\n&c- ${user.first}&r"
            }
        }

        response.replaceFirst("\n", "")

        sendMessage(response, player)
        return true
    }

    private fun sendMessage(message: String, player: CommandSender) {
        val formattedMessage = ChatColor.translateAlternateColorCodes('&', message)
        if (player is Player) {
            player.sendMessage(formattedMessage)
        } else {
            plugin.server.consoleSender.sendMessage(formattedMessage)
        }
    }
}
