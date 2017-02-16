package gg.obsidian.discordbridge

import com.michaelwflaherty.cleverbotapi.CleverBotQuery
import java.io.IOException

object CommandLogic {

    fun askCleverbot(key: String, message: String): String {

        if (key.isEmpty()) return "You do not have an API key. Go to https://www.cleverbot.com/api/ for more information."
        val bot: CleverBotQuery = CleverBotQuery(key, message)
        var response: String
        try {
            bot.sendRequest()
            response = bot.response
        } catch (e: IOException) {
            response = e.message!!
        }

        return response
    }

}
