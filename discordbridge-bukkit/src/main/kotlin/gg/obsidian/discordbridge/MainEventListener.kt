package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.wrapper.DbBukkitConsoleSender
import gg.obsidian.discordbridge.wrapper.DbBukkitPlayer
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.dynmap.DynmapWebChatEvent

class MainEventListener: Listener, CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return if (sender is org.bukkit.entity.Player) {
            val wp = DbBukkitPlayer(sender)
            DiscordBridge.handleCommand(wp, command.name, args)
        } else {
            DiscordBridge.handleCommand(DbBukkitConsoleSender(Bukkit.getConsoleSender()), command.name, args)
        }
    }

    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent) {
        event.message = DiscordBridge.handlePlayerChat(DbBukkitPlayer(event.player), event.message, event.isCancelled)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        DiscordBridge.handlePlayerJoin(DbBukkitPlayer(event.player))
    }

    @EventHandler
    fun onPlayerLeave(event: PlayerQuitEvent) {
        DiscordBridge.handlePlayerQuit(DbBukkitPlayer(event.player))
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity.player
        val msg = event.deathMessage
        if (player != null && msg != null) {
            DiscordBridge.handlePlayerDeath(DbBukkitPlayer(player), msg)
        }
    }
}
