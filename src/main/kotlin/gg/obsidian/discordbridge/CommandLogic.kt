package gg.obsidian.discordbridge

import com.michaelwflaherty.cleverbotapi.CleverBotQuery
import gg.obsidian.discordbridge.Utils.Rating
import gg.obsidian.discordbridge.Utils.Respect
import java.io.IOException
import java.util.*

object CommandLogic {

    fun askCleverbot(plugin: Plugin, message: String): String {
        if (plugin.cfg.CLEVERBOT_KEY.isEmpty())
            return "You do not have an API key. Go to https://www.cleverbot.com/api/ for more information."
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
        val responses = plugin.eightball.data.getStringList("responses")
        val rand = Random().nextInt(responses.count())
        return "$name - ${responses[rand]}"
    }

    fun f(plugin: Plugin, sender_name: String): String {
        var totalRespects = plugin.f.data.getInt("total-respects", 0)
        val responses = plugin.f.data.getList("responses").checkItemsAre<Respect>()
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
        val msg = found.message.replace("%u", sender_name).replace("%t", totalRespects.toString())
                .replace("%c", found.count.toString())

        plugin.f.data.set("total-respects", totalRespects)
        plugin.f.saveConfig()

        return msg
    }

    fun insult(plugin: Plugin, sender_name: String, arg: String): String {
        val responses = plugin.insult.data.getStringList("responses")
        val rand = Random().nextInt(responses.count())
        return plugin.insult.data.getString("template", "").replace("%u", sender_name)
                .replace("%i", responses[rand]).replace("%t", arg)
    }

    fun rate(plugin: Plugin, sender_name: String, arg: String): String {
        val responses = plugin.rate.data.getList("responses").checkItemsAre<Rating>()
                ?: return "ERROR: Responses for this command could not be read from the config."

        var rateOutOf = plugin.rate.data.getInt("rate-out-of", 10)
        if (rateOutOf > 1000000) rateOutOf = 1000000
        if (rateOutOf < 0) rateOutOf = 0

        var granularity = plugin.rate.data.getInt("granularity", 1)
        if (granularity > 2) granularity = 2
        if (granularity < 0) granularity = 0

        val conversionFactor = Math.pow(10.0, granularity.toDouble())
        val rating = Random().nextInt((rateOutOf * conversionFactor.toInt()) + 1) / conversionFactor


        val found: Rating = responses.firstOrNull { rating <= it.high && rating >= it.low }
                ?: return "ERROR: No response set for rating $rating"

        var thingToBeRated = arg
        if (plugin.rate.data.getBoolean("translate-first-and-second-person", true)) {
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

    // UTIL

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> List<*>.checkItemsAre() = if (all { it is T }) this as List<T> else null

}
