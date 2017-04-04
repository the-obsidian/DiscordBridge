package co.orre.discordbridge.minecraft.commands

import co.orre.discordbridge.CommandLogic
import co.orre.discordbridge.Config
import co.orre.discordbridge.minecraft.Permissions
import co.orre.discordbridge.Plugin
import co.orre.discordbridge.discord.Connection
import co.orre.discordbridge.utils.UtilFunctions.toMinecraftChatMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class Insult(val plugin: Plugin) : CommandExecutor {
    override fun onCommand(player: CommandSender, cmd: Command, alias: String?, args: Array<out String>?): Boolean {
        if (args == null || args.isEmpty()) return false

        if (player is Player && Permissions.insult.has(player)) {
            var msg = CommandLogic.insult(plugin, player.name, args.joinToString(" "))
            plugin.sendToMinecraft(msg.toMinecraftChatMessage(Config.BOT_MC_USERNAME))
            msg = plugin.translateAliasToDiscord(msg, player.uniqueId.toString())
            plugin.sendToDiscord(msg, Connection.getRelayChannel())
        }
        return true
    }
}