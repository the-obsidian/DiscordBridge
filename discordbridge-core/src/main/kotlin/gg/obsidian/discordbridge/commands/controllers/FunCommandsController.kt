package gg.obsidian.discordbridge.commands.controllers

import com.michaelwflaherty.cleverbotapi.CleverBotQuery
import gg.obsidian.discordbridge.DiscordBridge
import gg.obsidian.discordbridge.commands.IEventWrapper
import gg.obsidian.discordbridge.commands.annotations.BotCommand
import gg.obsidian.discordbridge.commands.annotations.TaggedResponse
import gg.obsidian.discordbridge.util.Cfg
import gg.obsidian.discordbridge.util.Rating
import gg.obsidian.discordbridge.util.Respect
import java.util.*
import kotlin.collections.LinkedHashMap

/**
 * Controller for fun commands that have no purpose outside of amusement
 */
class FunCommandsController : IBotController {

    /**
     * @return a description of this class of commands, used in the Help command
     */
    override fun getDescription(): String = ":balloon:  **FUN** - Every bot has to have them!"

    /**
     * Answers a yes/no question
     *
     * @param event the incoming event object
     * @return the response string
     */
    @BotCommand(
            aliases = ["8ball", "eightball", "8"],
            usage = "<question>",
            desc = "Consult the Magic 8 Ball",
            ignoreExcessArgs = true
    )
    @TaggedResponse
    private fun eightBall(event: IEventWrapper): String {
        DiscordBridge.logDebug("user ${event.senderName} consults the Magic 8-Ball")
        val responses = DiscordBridge.getConfig(Cfg.EBALL).getList<String>("responses")
        val rand = Random().nextInt(responses.count())
        return responses[rand]
    }

    /**
     * Makes a selection among an arbitrary number of supplied choices
     *
     * @param event the incoming event object
     * @param query a delimited string of the options to choose from
     * @return the response string
     */
    @BotCommand(
            aliases = ["choose"],
            usage = "<option>, <option>, ... (delimiters include ',', 'or', '|')",
            desc = "Have the bot choose between given options",
            squishExcessArgs = true
    )
    @TaggedResponse
    private fun choose(event: IEventWrapper, query: String): String {
        DiscordBridge.logDebug("user ${event.senderName} needs a choice made")
        val choices = query.split(", or ", " or ", ",", "|")
        val rand = Random().nextInt(choices.count())
        return "I pick '${choices[rand].trim()}'"
    }

    /**
     * Press F to pay respects
     *
     * @param event the incoming event object
     * @return the response string
     */
    @BotCommand(
            aliases = ["f"],
            desc = "Press F to pay respects"
    )
    private fun f(event: IEventWrapper): String {
        DiscordBridge.logDebug("user ${event.senderName}} pays respects")
        var totalRespects = DiscordBridge.getConfig(Cfg.F).getInteger("total-respects", 0)
        val responses = DiscordBridge.getConfig(Cfg.F).getList<LinkedHashMap<String, Any>>("responses").castTo({Respect(it)})
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
        val msg: String
        msg = if (found.message.contains("%u"))
            found.message.replace("%u", event.senderAsMention).replace("%t", totalRespects.toString())
                    .replace("%c", found.count.toString())
        else
            "${event.senderAsMention} | ${found.message}".replace("%t", totalRespects.toString())
                    .replace("%c", found.count.toString())

        DiscordBridge.getConfig(Cfg.F).put("total-respects", totalRespects)
        DiscordBridge.getConfig(Cfg.F).save()

        return msg
    }

    /**
     * Insult someone or something
     *
     * @param event the incoming event object
     * @param thingToInsult the target of the insult
     * @return the response string
     */
    @BotCommand(
            aliases = ["insult"],
            usage = "<thing to insult>",
            desc = "Make the bot insult something for you",
            squishExcessArgs = true
    )
    private fun insult(event: IEventWrapper, thingToInsult: String): String {
        DiscordBridge.logDebug("user ${event.senderName} requests an insult against '$thingToInsult'")
        val responses = DiscordBridge.getConfig(Cfg.INSULT).getList<String>("responses")
        val rand = Random().nextInt(responses.count())
        return "${event.senderAsMention} \u27A4 $thingToInsult | ${responses[rand]}"
    }

    /**
     * Rate something on a scale specified by the config
     *
     * @param event the incoming event object
     * @param thingToRate the thing that will receive the rating
     * @return the response string
     */
    @BotCommand(
            aliases = ["rate"],
            usage = "<thing to be rated>",
            desc = "Have the bot rate something for you",
            squishExcessArgs = true
    )
    @TaggedResponse
    private fun rate(event: IEventWrapper, thingToRate: String): String {
        DiscordBridge.logDebug("user ${event.senderName} requests a rating")
        val responses = DiscordBridge.getConfig(Cfg.RATE).getList<LinkedHashMap<String, Any>>("responses").castTo({Rating(it)})

        var rateOutOf = DiscordBridge.getConfig(Cfg.RATE).getInteger("rate-out-of", 10)
        if (rateOutOf > 1000000) rateOutOf = 1000000
        if (rateOutOf < 0) rateOutOf = 0

        var granularity = DiscordBridge.getConfig(Cfg.RATE).getInteger("granularity", 1)
        if (granularity > 2) granularity = 2
        if (granularity < 0) granularity = 0

        val conversionFactor = Math.pow(10.0, granularity.toDouble())
        val rating = Random().nextInt((rateOutOf * conversionFactor.toInt()) + 1) / conversionFactor

        val found: Rating = responses.firstOrNull { rating <= it.high && rating >= it.low }
                ?: return "ERROR: No response set for rating $rating"

        var thingToBeRated = thingToRate
        if (DiscordBridge.getConfig(Cfg.RATE).getBoolean("translate-first-and-second-person", true)) {
            val argArray = thingToRate.split(" ").toMutableList()
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

        return found.message.replace("%m", thingToBeRated).replace("%r", "$rating/$rateOutOf")
    }

    /**
     * Roll a die with a specified number of sides
     *
     * @param event the incoming event object
     * @param sides the number of sides of the die
     * @return the response string
     */
    @BotCommand(
            aliases = ["roll"],
            usage = "<sides>",
            desc = "Roll a die for a random number"
    )
    @TaggedResponse
    private fun roll(event: IEventWrapper, sides: Int): String {
        DiscordBridge.logDebug("user ${event.senderName} needs a choice made")
        if (sides == 1)
            return "You rolled... 1. Was it any surprise?"
        if (sides > 100 || sides < 1)
            return "I can't roll a die with $sides sides. It must have between 1 and 100 sides."
        val rand = Random().nextInt(sides)
        return "You rolled... $rand"
    }

    /**
     * Talk to Cleverbot
     *
     * @param event the incoming event object
     * @param query the message to send to Cleverbot
     * @return the response string
     */
    @TaggedResponse
    @BotCommand(
            aliases = ["talk", "cleverbot"],
            usage = "<say something>",
            desc = "Say something to Cleverbot",
            squishExcessArgs = true
    )
    private fun talk(event: IEventWrapper, query: String): String {
        DiscordBridge.logDebug("user ${event.senderName} invokes Cleverbot")

        if (DiscordBridge.getConfig(Cfg.CONFIG).getString("cleverbot-key", "").isEmpty())
            return "You do not have an API key. Go to https://www.cleverbot.com/JDA/ for more information."
        val bot = CleverBotQuery(DiscordBridge.getConfig(Cfg.CONFIG).getString("cleverbot-key", ""), query)
        bot.sendRequest()
        return bot.response
    }

    private inline fun <reified T : Any> List<HashMap<String, Any>>.castTo(factory: (HashMap<String, Any>) -> T): List<T> {
        return this.mapTo(mutableListOf()) { factory(it) }.toList()
    }
}
