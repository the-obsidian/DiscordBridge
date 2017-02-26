package gg.obsidian.discordbridge.Utils

import org.bukkit.ChatColor

fun String.noSpace() = this.replace(Regex("""\s+"""), "")
fun String.stripColor(): String = ChatColor.stripColor(this)