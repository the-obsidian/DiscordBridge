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
import org.bukkit.command.Command
import org.bukkit.command.SimpleCommandMap
import org.bukkit.plugin.SimplePluginManager
import java.util.*
import java.util.logging.Level

class DbBukkitServer(private val plugin: BukkitDiscordBridge, private val bukkitServer: Server) : IDbServer {
    lateinit var knownCommands: List<String>

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

    override fun getAllCommandNames(): List<String> {
        if (::knownCommands.isInitialized)
            return knownCommands
        try {
            val pm = bukkitServer.pluginManager as SimplePluginManager?
            if (pm != null) {
                val cmapField = pm::class.java.getDeclaredField("commandMap")
                cmapField.isAccessible = true
                val cmap = cmapField.get(pm) as SimpleCommandMap
                val knownCommandsField = cmap::class.java.getDeclaredField("knownCommands")
                knownCommandsField.isAccessible = true
                val kc = (knownCommandsField.get(cmap) as Map<String, Command>).keys.toList()
                knownCommands = kc
                return kc
            }
            plugin.logger.warning("Could not get command list - plugin manager is null")
            return listOf()
        }
        catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Could not get command list", e)
            return listOf()
        }
    }
}
