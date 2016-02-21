package gg.obsidian.discordbridge

import org.bukkit.ChatColor

object Util {
    fun formatMessage(message: String, replacements: Map<String, String>, colors: Boolean = false): String {
        var formattedString = message

        if (colors) formattedString = ChatColor.translateAlternateColorCodes('&', formattedString)

        for ((token, replacement) in replacements) {
            formattedString = formattedString.replace(token, replacement)
        }

        return formattedString
    }
}
