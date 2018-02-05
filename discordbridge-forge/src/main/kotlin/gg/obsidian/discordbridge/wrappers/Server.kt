package gg.obsidian.discordbridge.wrappers

import gg.obsidian.discordbridge.DiscordBridgeForge
import net.dv8tion.jda.core.entities.MessageChannel
import net.minecraft.util.text.TextComponentString
import net.minecraftforge.common.ForgeVersion
import net.minecraftforge.fml.common.FMLCommonHandler
import java.util.*

class Server(private val mod: DiscordBridgeForge) : IServer {
    override fun getScheduler(): IScheduler {
        return Scheduler()
    }

    // TODO
    override fun getVersion(): String {
        return "Minecraft Forge " + ForgeVersion.mcVersion
    }

    override fun getMinecraftShortVersion(): String {
        return "Minecraft Forge " + ForgeVersion.mcVersion
    }

    override fun getPlayer(uuid: UUID): IPlayer? {
        val p = FMLCommonHandler.instance().minecraftServerInstance.playerList.getPlayerByUUID(uuid)
        return Player(p)
    }

    override fun getPlayer(name: String): IPlayer? {
        val p = FMLCommonHandler.instance().minecraftServerInstance.playerList.getPlayerByUsername(name)
        return if (p != null) Player(p) else null
    }

    override fun getOnlinePlayers(): List<IPlayer> {
        return FMLCommonHandler.instance().minecraftServerInstance.playerList.players.map { Player(it) }
    }

    override fun broadcastMessage(message: String) {
        FMLCommonHandler.instance().minecraftServerInstance.playerList.sendMessage(TextComponentString(message))
    }

    override fun getRemoteConsoleSender(): IConsoleSender {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun dispatchCommand(channel: MessageChannel, command: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLogger(): Logger {
        return Logger(mod.logger)
    }

}