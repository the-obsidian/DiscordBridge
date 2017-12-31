package gg.obsidian.discordbridge.wrappers

import gg.obsidian.discordbridge.DiscordBridge
import net.dv8tion.jda.core.entities.MessageChannel
import org.bukkit.command.ConsoleCommandSender
import java.util.*

class ConsoleSender(val db: DiscordBridge, val bukkitConsoleSender: ConsoleCommandSender) : IConsoleSender {

    override fun sendMessage(message: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasPermission(permission: String): Boolean {
        return bukkitConsoleSender.hasPermission(permission)
    }

    override fun getUUID(): UUID {
        TODO("not implemented")
    }

    override fun getName(): String {
        return "DiscordRemote"
    }

}