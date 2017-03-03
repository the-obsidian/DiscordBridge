package gg.obsidian.discordbridge.Utils

import org.bukkit.ChatColor

fun String.noSpace() = this.replace(Regex("""\s+"""), "")
fun String.stripColor(): String = ChatColor.stripColor(this)

data class UserAlias(var mcUsername: String, var mcUuid: String,
                     var discordUsername: String, var discordId: String)