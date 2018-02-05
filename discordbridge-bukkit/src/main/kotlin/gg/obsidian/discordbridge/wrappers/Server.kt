package gg.obsidian.discordbridge.wrappers

import gg.obsidian.discordbridge.DiscordBridgePlugin
import gg.obsidian.discordbridge.commands.DiscordCommandSender
import org.bukkit.Bukkit
import org.bukkit.Server
import java.util.*

class Server(val plugin: DiscordBridgePlugin, val bukkitServer: Server) : IServer {
    override fun getScheduler(): IScheduler {
        return Scheduler(plugin, bukkitServer.scheduler)
    }

    override fun getVersion(): String {
        return bukkitServer.bukkitVersion.split("-")[0]
    }

    override fun getMinecraftShortVersion(): String {
        return bukkitServer.bukkitVersion.split("-")[0]
    }

    override fun getPlayer(uuid: UUID): IPlayer? {
        return Player(bukkitServer.getPlayer(uuid))
    }

    override fun getPlayer(name: String): IPlayer? {
        return Player(bukkitServer.getPlayer(name))
    }

    override fun getOnlinePlayers(): List<IPlayer> {
        return bukkitServer.onlinePlayers.map { Player(it) }
    }

    override fun broadcastMessage(message: String) {
        bukkitServer.broadcastMessage(message)
    }

    override fun dispatchCommand(sender: DiscordCommandSender, command: String) {
        val rcon = DiscordRConConsoleSource(sender, Bukkit.getServer().consoleSender)
        bukkitServer.dispatchCommand(rcon, command)
    }

    override fun getLogger(): ILogger {
        return Logger(plugin.logger)
    }

}