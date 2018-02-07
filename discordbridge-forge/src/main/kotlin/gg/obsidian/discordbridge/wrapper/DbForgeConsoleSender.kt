package gg.obsidian.discordbridge.wrapper

import net.minecraft.network.rcon.RConConsoleSource
import net.minecraft.util.text.TextComponentString
import java.util.*

class DbForgeConsoleSender(private val sender: RConConsoleSource) : IDbConsoleSender {
    override fun getName(): String {
        return sender.name
    }

    override fun getUUID(): UUID {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun sendMessage(message: String) {
        sender.sendMessage(TextComponentString(message))
    }

    override fun hasPermission(permission: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
