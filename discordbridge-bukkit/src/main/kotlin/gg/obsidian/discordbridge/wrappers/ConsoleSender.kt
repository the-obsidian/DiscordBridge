package gg.obsidian.discordbridge.wrappers

import org.bukkit.command.ConsoleCommandSender
import java.util.*

class ConsoleSender(val bukkitConsoleSender: ConsoleCommandSender) : IConsoleSender {

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