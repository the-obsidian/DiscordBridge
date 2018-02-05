package gg.obsidian.discordbridge.wrappers

import gg.obsidian.discordbridge.DiscordBridgePlugin
import gg.obsidian.discordbridge.commands.DiscordCommandSender
import gg.obsidian.discordbridge.util.unwrap
import org.spongepowered.api.Game
import org.spongepowered.api.Sponge
import org.spongepowered.api.text.Text
import java.util.*

class Server(private val plugin: DiscordBridgePlugin, private val game: Game) : IServer {
    override fun getScheduler(): IScheduler {
        return Scheduler(plugin, game)
    }

    override fun getVersion(): String {
        return game.platform.minecraftVersion.name
    }

    override fun getMinecraftShortVersion(): String {
        return game.platform.minecraftVersion.name
    }

    override fun getPlayer(uuid: UUID): IPlayer? {
        val p = Sponge.getServer().getPlayer(uuid).unwrap()
        return if (p != null) Player(p) else null
    }

    override fun getPlayer(name: String): IPlayer? {
        val p = Sponge.getServer().getPlayer(name).unwrap()
        return if (p != null) Player(p) else null
    }

    override fun getOnlinePlayers(): List<IPlayer> {
        return Sponge.getServer().onlinePlayers.map { Player(it) }
    }

    override fun broadcastMessage(message: String) {
        Sponge.getServer().broadcastChannel.send(Text.of(message))
    }

    override fun dispatchCommand(sender: DiscordCommandSender, command: String) {
        val rcon = DiscordRconConsoleSource(sender, game.server.console)
        Sponge.getCommandManager().process(rcon, command)
    }

    override fun getLogger(): Logger {
        return Logger(plugin.getLogger())
    }

}