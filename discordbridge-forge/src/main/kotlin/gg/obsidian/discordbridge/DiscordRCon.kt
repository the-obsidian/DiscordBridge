package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.commands.DiscordCommandSender
import net.minecraft.network.rcon.RConConsoleSource
import net.minecraft.server.MinecraftServer
import net.minecraft.util.text.ITextComponent

class DiscordRCon(val sender: DiscordCommandSender, serverIn: MinecraftServer) : RConConsoleSource(serverIn) {
    override fun getName(): String {
        return sender.senderName
    }

    override fun sendMessage(component: ITextComponent) {
        sender.sendMessage(component.unformattedText)
    }
}