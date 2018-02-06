package gg.obsidian.discordbridge.wrappers

import java.util.*

interface IDbCommandSender {
    fun getName(): String
    fun getUUID(): UUID
    fun sendMessage(message: String)
    fun hasPermission(permission: String): Boolean
}