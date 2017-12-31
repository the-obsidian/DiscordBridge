package gg.obsidian.discordbridge.wrappers

import net.dv8tion.jda.core.entities.MessageChannel
import java.util.*
import java.util.logging.Logger

interface IServer {

    fun getScheduler(): IScheduler

    fun getVersion(): String

    fun getMinecraftShortVersion(): String

    fun getPlayer(uuid: UUID): IPlayer

    fun getPlayer(name: String): IPlayer

    fun getOnlinePlayers(): List<IPlayer>

    fun broadcastMessage(message: String)

    fun getRemoteConsoleSender(): IConsoleSender

    fun dispatchCommand(channel: MessageChannel, command: String)

    fun getLogger(): Logger

}