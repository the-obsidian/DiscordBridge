package gg.obsidian.discordbridge.wrappers

import gg.obsidian.discordbridge.DiscordBridgePlugin
import gg.obsidian.discordbridge.DiscordCommandSender
import net.dv8tion.jda.core.entities.MessageChannel
import org.bukkit.Server
import java.util.*

class Server(val plugin: DiscordBridgePlugin, val bukkitServer: Server) : IServer {
    override fun getRemoteConsoleSender(): IConsoleSender {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getScheduler(): IScheduler {
        return Scheduler(plugin, bukkitServer.scheduler)
    }

    override fun getVersion(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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

    override fun dispatchCommand(channel: MessageChannel, command: String) {
        val sender = DiscordCommandSender(channel)
        bukkitServer.dispatchCommand(sender, command)
    }

    override fun getLogger(): ILogger {
        return Logger(plugin.logger)
    }

}