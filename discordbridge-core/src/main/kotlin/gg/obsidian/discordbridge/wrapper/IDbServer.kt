package gg.obsidian.discordbridge.wrapper

import gg.obsidian.discordbridge.command.DiscordCommandSender
import gg.obsidian.discordbridge.util.UrlAttachment
import java.util.*

interface IDbServer {

    fun getScheduler(): IDbScheduler

    fun getMinecraftVersion(): String

    fun getPlayer(uuid: UUID): IDbPlayer?

    fun getPlayer(name: String): IDbPlayer?

    fun getOnlinePlayers(): List<IDbPlayer>

    fun broadcastMessage(message: String)

    fun broadcastAttachment(att: UrlAttachment)

    fun dispatchCommand(sender: DiscordCommandSender, command: String)

    fun getLogger(): IDbLogger

}