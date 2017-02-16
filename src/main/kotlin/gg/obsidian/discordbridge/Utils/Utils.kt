package gg.obsidian.discordbridge.Utils

import org.bukkit.ChatColor

fun String.noSpace() = this.replace(Regex("""\s+"""), "")
fun String.stripColor() = ChatColor.stripColor(this)

object Utils {

    fun formatMessage(message: String, replacements: Map<String, String>, colors: Boolean = false): String {
        var formattedString = message

        for ((token, replacement) in replacements) {
            formattedString = formattedString.replace(token, replacement)
        }

        if (colors) formattedString = ChatColor.translateAlternateColorCodes('&', formattedString)

        return formattedString
    }

}
