package gg.obsidian.discordbridge

import org.bukkit.ChatColor
import java.io.IOException
import com.michaelwflaherty.cleverbotapi.CleverBotQuery

object Util {

    fun formatMessage(message: String, replacements: Map<String, String>, colors: Boolean = false): String {
        var formattedString = message

        for ((token, replacement) in replacements) {
            formattedString = formattedString.replace(token, replacement)
        }

        if (colors) formattedString = ChatColor.translateAlternateColorCodes('&', formattedString)

        return formattedString
    }

    fun askCleverbot(key: String, message: String): String {

        if (key.isEmpty()) return "You do not have an API key. Go to https://www.cleverbot.com/api/ for more information."
        val bot: CleverBotQuery = CleverBotQuery(key, message)
        var response: String
        try
        {
            bot.sendRequest()
            response = bot.response
        }
        catch (e: IOException)
        {
            response = e.message!!
        }

        return response
    }
}
