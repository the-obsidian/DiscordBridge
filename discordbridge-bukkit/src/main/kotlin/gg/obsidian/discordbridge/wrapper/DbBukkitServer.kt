package gg.obsidian.discordbridge.wrapper

import gg.obsidian.discordbridge.BukkitDiscordBridge
import gg.obsidian.discordbridge.DiscordRCon
import gg.obsidian.discordbridge.command.DiscordCommandSender
import gg.obsidian.discordbridge.util.UrlAttachment
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.Server
import java.util.*

class DbBukkitServer(private val plugin: BukkitDiscordBridge, private val bukkitServer: Server) : IDbServer {
    override fun broadcastAttachment(att: UrlAttachment) {
        val msg = ComponentBuilder("${att.sender} sent ")
                .color(net.md_5.bungee.api.ChatColor.ITALIC)
                .append("an attachment")
                .color(net.md_5.bungee.api.ChatColor.RESET)
                .color(net.md_5.bungee.api.ChatColor.UNDERLINE)
                .event(ClickEvent(ClickEvent.Action.OPEN_URL, att.url))
                .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder(att.hoverText).create()))
                .create()
        bukkitServer.spigot().broadcast(*msg)
    }

    override fun getScheduler(): IDbScheduler {
        return DbBukkitScheduler(plugin, bukkitServer.scheduler)
    }

    override fun getMinecraftVersion(): String {
        return bukkitServer.bukkitVersion.split("-")[0]
    }

    override fun getPlayer(uuid: UUID): IDbPlayer? {
        return DbBukkitPlayer(bukkitServer.getOfflinePlayer(uuid))
    }

    override fun getOnlinePlayers(): List<IDbPlayer> {
        return bukkitServer.onlinePlayers.map { DbBukkitPlayer(it) }
    }

    override fun broadcastMessage(message: String) {
        bukkitServer.broadcastMessage(message)
    }

    override fun dispatchCommand(sender: DiscordCommandSender, command: String) {
        val rcon = DiscordRCon(sender, Bukkit.getServer().consoleSender)
        bukkitServer.dispatchCommand(rcon, command)
    }

    override fun getLogger(): IDbLogger {
        return DbBukkitLogger(plugin.logger)
    }
}
