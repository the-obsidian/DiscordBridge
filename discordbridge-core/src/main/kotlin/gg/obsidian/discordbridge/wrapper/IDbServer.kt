package gg.obsidian.discordbridge.wrapper

import gg.obsidian.discordbridge.command.DiscordCommandSender
import gg.obsidian.discordbridge.util.UrlAttachment
import java.util.*

interface IDbServer {
    fun broadcastAttachment(att: UrlAttachment)

    fun broadcastMessage(message: String)

    fun dispatchCommand(sender: DiscordCommandSender, command: String)

    fun getLogger(): IDbLogger

    fun getMinecraftVersion(): String

    fun getOnlinePlayers(): List<IDbPlayer>

    fun getPlayer(uuid: UUID): IDbPlayer?

    fun getScheduler(): IDbScheduler

    // https://bukkit.org/threads/get-all-the-available-commands.61941/
    fun getAllCommandNames(): List<String>
}
