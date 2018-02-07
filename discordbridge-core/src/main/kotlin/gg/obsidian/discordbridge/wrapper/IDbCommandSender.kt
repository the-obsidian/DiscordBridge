package gg.obsidian.discordbridge.wrapper

import java.util.*

interface IDbCommandSender {
    fun getName(): String

    fun getUUID(): UUID

    fun hasPermission(permission: String): Boolean

    fun sendMessage(message: String)
}
