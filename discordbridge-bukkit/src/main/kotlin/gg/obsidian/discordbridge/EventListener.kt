package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.wrappers.ConsoleSender
import gg.obsidian.discordbridge.wrappers.Player
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

class EventListener(val db: DiscordBridge) : Listener, CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return if (sender is org.bukkit.entity.Player) {
            val wp = Player(sender)
            db.handleCommand(wp, gg.obsidian.discordbridge.wrappers.Command(command), args)
        } else {
            db.handleCommand(ConsoleSender(db, Bukkit.getConsoleSender()), gg.obsidian.discordbridge.wrappers.Command(command), args)
        }
    }

    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent) {
        event.message = db.handlePlayerChat(Player(event.player), event.message, event.isCancelled)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        db.handlePlayerJoin(Player(event.player))
    }

    @EventHandler
    fun onPlayerLeave(event: PlayerQuitEvent) {
        db.handlePlayerQuit(Player(event.player))
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        db.handlePlayerDeath(Player(event.entity.player), event.deathMessage)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onDynmapCatEvent(event: DynmapWebChatEvent) {
        db.handleDynmapChat(event.name, event.message)
    }

}