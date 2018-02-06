package gg.obsidian.discordbridge.wrappers

import gg.obsidian.discordbridge.commands.DiscordCommandSender
import gg.obsidian.discordbridge.util.UrlAttachment
import java.util.*

interface IDbServer {

    fun getScheduler(): IDbScheduler

    fun getVersion(): String

    fun getMinecraftShortVersion(): String

    fun getPlayer(uuid: UUID): IDbPlayer?

    fun getPlayer(name: String): IDbPlayer?

    fun getOnlinePlayers(): List<IDbPlayer>

    fun broadcastMessage(message: String)

    fun broadcastAttachment(att: UrlAttachment)

    fun dispatchCommand(sender: DiscordCommandSender, command: String)

    fun getLogger(): IDbLogger

}