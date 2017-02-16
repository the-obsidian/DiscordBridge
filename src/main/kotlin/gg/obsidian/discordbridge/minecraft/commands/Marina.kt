package gg.obsidian.discordbridge.minecraft.commands

import gg.obsidian.discordbridge.CommandLogic
import gg.obsidian.discordbridge.Plugin
import gg.obsidian.discordbridge.Utils.*
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class Marina(val plugin: Plugin) : CommandExecutor {

    override fun onCommand(player: CommandSender, cmd: Command, alias: String?, args: Array<out String>?): Boolean {
        if (args == null || args.isEmpty()) return false

        if (player is Player) {
            val message: String = args.joinToString(" ")
            player.chat("@${plugin.cfg.USERNAME.noSpace()} $message")
            val response: String = CommandLogic.askCleverbot(plugin.cfg.CLEVERBOT_KEY, message)
            val formattedMessage = Utils.formatMessage(
                    plugin.cfg.TEMPLATES_MINECRAFT_CHAT_MESSAGE,
                    mapOf(
                            "%u" to plugin.cfg.USERNAME_COLOR
                                    + plugin.cfg.USERNAME.noSpace() + "&r",
                            "%m" to response
                    ),
                    colors = true
            )
            plugin.server.broadcastMessage(formattedMessage)
            plugin.sendToDiscord(response)
            return true
        }
        return true
    }
}