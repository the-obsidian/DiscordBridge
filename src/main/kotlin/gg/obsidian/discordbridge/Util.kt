package gg.obsidian.discordbridge

import org.bukkit.ChatColor

object Util {
    fun formatMessage(message: String, replacements: Map<String, String>, colors: Boolean = false): String {
        var formattedString = message
        for ((token, replacement) in replacements) {
            formattedString = formattedString.replace(token, replacement)
        }

        if (colors) formattedString = ChatColor.translateAlternateColorCodes('&', formattedString)

        return formattedString
    }
}
