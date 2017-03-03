package gg.obsidian.discordbridge.minecraft.commands

import gg.obsidian.discordbridge.CommandLogic
import gg.obsidian.discordbridge.Permissions
import gg.obsidian.discordbridge.Plugin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class Rate(val plugin: Plugin) : CommandExecutor {
    override fun onCommand(player: CommandSender, cmd: Command, alias: String?, args: Array<out String>?): Boolean {
        if (args == null || args.isEmpty()) return false

        if (player is Player && Permissions.rate.has(player)) {
            var msg = CommandLogic.rate(player.name, args.joinToString(" "))
            plugin.sendToMinecraft(plugin.toMinecraftChatMessage(msg, plugin.cfg.BOT_MC_USERNAME))
            msg = plugin.translateAliasToDiscord(msg, player.uniqueId.toString())
            plugin.sendToDiscord(msg, plugin.conn.getRelayChannel())
        }
        return true
    }
}
