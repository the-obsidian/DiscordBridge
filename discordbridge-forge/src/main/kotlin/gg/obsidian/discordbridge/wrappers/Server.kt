package gg.obsidian.discordbridge.wrappers

import gg.obsidian.discordbridge.DiscordBridgeForge
import gg.obsidian.discordbridge.commands.DiscordCommandSender
import gg.obsidian.discordbridge.util.UrlAttachment
import net.dv8tion.jda.core.entities.MessageChannel
import net.minecraft.util.text.TextComponentBase
import net.minecraft.util.text.TextComponentString
import net.minecraft.util.text.event.ClickEvent
import net.minecraft.util.text.event.HoverEvent
import net.minecraftforge.common.ForgeVersion
import net.minecraftforge.fml.common.FMLCommonHandler
import java.util.*

class Server(private val mod: DiscordBridgeForge) : IServer {
    override fun broadcastAttachment(att: UrlAttachment) {
        val pt1 = TextComponentString("${att.sender} sent ")
        pt1.style.italic = true
        val pt2 = TextComponentString("an attachment")
        pt2.style.underlined = true
        pt2.style.clickEvent = ClickEvent(ClickEvent.Action.OPEN_URL, att.url)
        pt2.style.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponentString(att.hoverText))

        FMLCommonHandler.instance().minecraftServerInstance.playerList.sendMessage(pt1.appendSibling(pt2))
    }

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

    override fun dispatchCommand(sender: DiscordCommandSender, command: String) {
        val rcon = DiscordRConConsoleSource(sender, FMLCommonHandler.instance().minecraftServerInstance)
        FMLCommonHandler.instance().minecraftServerInstance.commandManager.executeCommand(rcon, command)
    }

    override fun getLogger(): Logger {
        return Logger(mod.logger)
    }

}