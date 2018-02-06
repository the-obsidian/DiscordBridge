package gg.obsidian.discordbridge.wrapper

import gg.obsidian.discordbridge.ForgeDiscordBridge
import gg.obsidian.discordbridge.DiscordRCon
import gg.obsidian.discordbridge.command.DiscordCommandSender
import gg.obsidian.discordbridge.util.UrlAttachment
import net.minecraft.util.text.TextComponentString
import net.minecraft.util.text.event.ClickEvent
import net.minecraft.util.text.event.HoverEvent
import net.minecraftforge.common.ForgeVersion
import net.minecraftforge.fml.common.FMLCommonHandler
import java.util.*

class DbForgeServer(private val mod: ForgeDiscordBridge) : IDbServer {
    override fun broadcastAttachment(att: UrlAttachment) {
        val pt1 = TextComponentString("${att.sender} sent ")
        pt1.style.italic = true
        val pt2 = TextComponentString("an attachment")
        pt2.style.underlined = true
        pt2.style.clickEvent = ClickEvent(ClickEvent.Action.OPEN_URL, att.url)
        pt2.style.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponentString(att.hoverText))

        FMLCommonHandler.instance().minecraftServerInstance.playerList.sendMessage(pt1.appendSibling(pt2))
    }

    override fun getScheduler(): IDbScheduler {
        return DbForgeScheduler()
    }

    // TODO
    override fun getVersion(): String {
        return "Minecraft Forge " + ForgeVersion.mcVersion
    }

    override fun getMinecraftShortVersion(): String {
        return "Minecraft Forge " + ForgeVersion.mcVersion
    }

    override fun getPlayer(uuid: UUID): IDbPlayer? {
        val p = FMLCommonHandler.instance().minecraftServerInstance.playerList.getPlayerByUUID(uuid)
        return DbForgePlayer(p)
    }

    override fun getPlayer(name: String): IDbPlayer? {
        val p = FMLCommonHandler.instance().minecraftServerInstance.playerList.getPlayerByUsername(name)
        return if (p != null) DbForgePlayer(p) else null
    }

    override fun getOnlinePlayers(): List<IDbPlayer> {
        return FMLCommonHandler.instance().minecraftServerInstance.playerList.players.map { DbForgePlayer(it) }
    }

    override fun broadcastMessage(message: String) {
        FMLCommonHandler.instance().minecraftServerInstance.playerList.sendMessage(TextComponentString(message))
    }

    override fun dispatchCommand(sender: DiscordCommandSender, command: String) {
        val rcon = DiscordRCon(sender, FMLCommonHandler.instance().minecraftServerInstance)
        FMLCommonHandler.instance().minecraftServerInstance.commandManager.executeCommand(rcon, command)
    }

    override fun getLogger(): DbForgeLogger {
        return DbForgeLogger(mod.logger)
    }

}