package gg.obsidian.discordbridge.wrappers

import gg.obsidian.discordbridge.commands.DiscordCommandSender
import gg.obsidian.discordbridge.util.UrlAttachment
import net.dv8tion.jda.core.entities.MessageChannel
import java.util.*

interface IServer {

    fun getScheduler(): IScheduler

    fun getVersion(): String

    fun getMinecraftShortVersion(): String

    fun getPlayer(uuid: UUID): IPlayer?

    fun getPlayer(name: String): IPlayer?

    fun getOnlinePlayers(): List<IPlayer>

    fun broadcastMessage(message: String)

    fun broadcastAttachment(att: UrlAttachment)

    fun dispatchCommand(sender: DiscordCommandSender, command: String)

    fun getLogger(): ILogger

}