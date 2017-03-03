package gg.obsidian.discordbridge

import com.michaelwflaherty.cleverbotapi.CleverBotQuery
import java.io.IOException
import java.util.*

object CommandLogic {

    fun askCleverbot(plugin: Plugin, message: String): String {
        if (plugin.cfg.CLEVERBOT_KEY.isEmpty()) return "You do not have an API key. Go to https://www.cleverbot.com/api/ for more information."
        val bot: CleverBotQuery = CleverBotQuery(plugin.cfg.CLEVERBOT_KEY, message)
        var response: String
        try {
            bot.sendRequest()
            response = bot.response
        } catch (e: IOException) {
            response = e.message!!
        }

        return response
    }

    fun eightBall(plugin: Plugin, name: String): String {
        val responses = plugin.memory.data.getStringList("8-ball-responses")
        val rand = Random().nextInt(responses.count())
        return "$name - ${responses[rand]}"
    }

    fun f(plugin: Plugin, name: String): String {
        var respects = plugin.memory.data.getInt("respects", 0)
        val msg: String
        val payed = Random().nextInt(100)
        when {
            payed == 99 -> {
                respects += 5
                msg = "$name breaks down and mournfully cries 5 respects out to the sky! (Total respects paid: $respects)"
            }
            payed > 95 -> {
                respects += 3
                msg = "$name manages to give 3 respects through their heavy sobbing! (Total respects paid: $respects)"
            }
            payed > 65 -> {
                respects += 2
                msg = "$name sheds a single tear and pays 2 respects! (Total respects paid: $respects)"
            }
            else -> {
                respects += 1
                msg = "$name solemnly pays respect! (Total respects paid: $respects)"
            }
        }
        plugin.memory.data.set("respects", respects)
        plugin.memory.saveConfig()

        return msg
    }

    fun insult(plugin: Plugin, arg: String): String {
        val responses = plugin.insults.data.getStringList("insults")
        val rand = Random().nextInt(500)
        return "$arg - ${responses[rand]}"
    }

    fun rate(name: String, arg: String): String {
        val argArray = arg.split(" ").toMutableList()
        val iterate = argArray.listIterator()
        while (iterate.hasNext()) {
            val oldValue = iterate.next()
            if (oldValue == "me") iterate.set("you")
            if (oldValue == "myself") iterate.set("yourself")
            if (oldValue == "my") iterate.set("your")
            if (oldValue == "your") iterate.set("my")
            if (oldValue == "yourself") iterate.set("myself")
        }
        val thingToBeRated = argArray.joinToString(" ")
        val rating = Random().nextInt(101)
        var response = "I rate $thingToBeRated "
        when (rating) {
            42 -> response += "4/20: 'smoke weed erryday'."
            78 -> response += "7.8/10: 'too much water'."
            69 -> response += "69/69."
            100 -> response += "a perfect 10/10, absolutely flawless in every regard."
            in 90..99 -> response += "a very high ${rating/10f}/10."
            in 80..89 -> response += "a high ${rating/10f}/10."
            in 70..79 -> response += "a decently high ${rating/10f}/10."
            in 60..69 -> response += "a good ${rating/10f}/10."
            in 50..59 -> response += "a solid ${rating/10f}/10."
            in 40..49 -> response += "a meh ${rating/10f}/10."
            in 30..39 -> response += "a paltry ${rating/10f}/10."
            in 20..29 -> response += "a pretty low ${rating/10f}/10."
            in 10..19 -> response += "a low ${rating/10f}/10."
            in 1..9 -> response += "a shitty ${rating/10f}/10."
            else -> response += "0/0: 'amazing'."
        }
        return "$name - $response"
    }

}
