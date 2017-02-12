package gg.obsidian.discordbridge

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class HandleMarina(val plugin: Plugin): CommandExecutor {

    override fun onCommand(player: CommandSender, cmd: Command, alias: String?, args: Array<out String>?): Boolean {
        if (args == null || args.isEmpty()) return false

        if (player is Player) {
            val message: String = args.joinToString(" ")
            player.chat("@MarinaFriend " + message)
            val response: String = Util.askCleverbot(plugin.configuration.CLEVERBOT_KEY, message)
            val formattedMessage = Util.formatMessage(
                    plugin.configuration.TEMPLATES_MINECRAFT_CHAT_MESSAGE,
                    mapOf(
                            "%u" to plugin.configuration.USERNAME_COLOR + plugin.configuration.USERNAME.replace("\\s+", "") + "&r",
                            "%m" to response
                    ),
                    colors = true
            )
            plugin.server.broadcastMessage(formattedMessage)
            plugin.sendToDiscordRelaySelf(response)
            return true
        }
        return true
    }
}