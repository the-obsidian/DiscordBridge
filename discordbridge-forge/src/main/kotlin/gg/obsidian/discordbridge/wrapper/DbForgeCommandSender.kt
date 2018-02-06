package gg.obsidian.discordbridge.wrapper

import net.minecraft.util.text.TextComponentString
import java.util.*

class DbForgeCommandSender(private val sender: net.minecraft.command.ICommandSender) : IDbCommandSender {
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
        // TODO
        // TODO
        // TODO
        // TODO
        // TODO
        return true
    }

}