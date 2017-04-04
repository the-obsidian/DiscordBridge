package co.orre.discordbridge

import com.michaelwflaherty.cleverbotapi.CleverBotQuery
import co.orre.discordbridge.utils.Rating
import co.orre.discordbridge.utils.Respect
import java.io.IOException
import java.util.*

object CommandLogic {

    fun askCleverbot(invokerName: String, query: String): String {
        if (Config.CLEVERBOT_KEY.isEmpty())
            return "You do not have an API key. Go to https://www.cleverbot.com/JDA/ for more information."
        val bot: CleverBotQuery = CleverBotQuery(Config.CLEVERBOT_KEY, query)
        var response: String
        try {
            bot.sendRequest()
            response = bot.response
        } catch (e: IOException) {
            response = e.message!!
        }

        return "$invokerName | $response"
    }

    fun choose(invokerName: String, query: String): String {
        val choices = query.split(", or ", " or ", ",", "|")
        val rand = Random().nextInt(choices.count())
        return "$invokerName | I pick '${choices[rand]}'"
    }

    fun eightBall(p: Plugin, invokerName: String): String {
        val responses = p.eightball.data.getStringList("responses")
        val rand = Random().nextInt(responses.count())
        return "$invokerName | ${responses[rand]}"
    }

    fun f(p: Plugin, invokerName: String): String {
        var totalRespects = p.f.data.getInt("total-respects", 0)
        val responses = p.f.data.getList("responses").checkItemsAre<Respect>()
                ?: return "ERROR: Responses for this command could not be read from the config."
        val totalWeight = responses.sumBy { it.weight }
        var rand = Random().nextInt(totalWeight) + 1
        var found: Respect? = null
        for(r in responses) {
            rand -= r.weight
            if (rand <= 0) {
                found = r
                break
            }
        }

        totalRespects += found!!.count
        val msg = found.message.replace("%u", invokerName).replace("%t", totalRespects.toString())
                .replace("%c", found.count.toString())

        p.f.data.set("total-respects", totalRespects)
        p.f.saveConfig()

        return msg
    }

    fun insult(p: Plugin, invokerName: String, thingToBeInsulted: String): String {
        val responses = p.insult.data.getStringList("responses")
        val rand = Random().nextInt(responses.count())
        return p.insult.data.getString("template", "").replace("%u", invokerName)
                .replace("%i", responses[rand]).replace("%t", thingToBeInsulted)
    }

    fun rate(p: Plugin, sender_name: String, arg: String): String {
        val responses = p.rate.data.getList("responses").checkItemsAre<Rating>()
                ?: return "ERROR: Responses for this command could not be read from the config."

        var rateOutOf = p.rate.data.getInt("rate-out-of", 10)
        if (rateOutOf > 1000000) rateOutOf = 1000000
        if (rateOutOf < 0) rateOutOf = 0

        var granularity = p.rate.data.getInt("granularity", 1)
        if (granularity > 2) granularity = 2
        if (granularity < 0) granularity = 0

        val conversionFactor = Math.pow(10.0, granularity.toDouble())
        val rating = Random().nextInt((rateOutOf * conversionFactor.toInt()) + 1) / conversionFactor


        val found: Rating = responses.firstOrNull { rating <= it.high && rating >= it.low }
                ?: return "ERROR: No response set for rating $rating"

        var thingToBeRated = arg
        if (p.rate.data.getBoolean("translate-first-and-second-person", true)) {
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
            thingToBeRated = argArray.joinToString(" ")
        }

        return found.message.replace("%u", sender_name).replace("%m", thingToBeRated).replace("%r", "$rating/$rateOutOf")
    }

    fun roll(invokerName: String, sides: Int): String {
        if (sides == 1)
            return "$invokerName | You rolled... 1. Was it any surprise?"
        if (sides > 100 || sides < 1)
            return "$invokerName | I can't roll a die with $sides sides. It must have between 1 and 100 sides."
        val rand = Random().nextInt(sides)
        return "$invokerName | You rolled... $rand"
    }

    // UTIL

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> List<*>.checkItemsAre() = if (all { it is T }) this as List<T> else null

}
