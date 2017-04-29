package gg.obsidian.discordbridge.commands.controllers

import com.michaelwflaherty.cleverbotapi.CleverBotQuery
import gg.obsidian.discordbridge.Config
import gg.obsidian.discordbridge.Plugin
import gg.obsidian.discordbridge.commands.IBotController
import gg.obsidian.discordbridge.commands.IEventWrapper
import gg.obsidian.discordbridge.commands.annotations.BotCommand
import gg.obsidian.discordbridge.commands.annotations.TaggedResponse
import gg.obsidian.discordbridge.utils.Rating
import gg.obsidian.discordbridge.utils.Respect
import java.util.*

/**
 * Controller for fun commands that have no purpose outside of amusement
 *
 * @param plugin a reference to the base Plugin object
 */
class FunCommandsController(val plugin: Plugin) : IBotController {

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
    @BotCommand(name = "8ball", usage = "<question>", description = "Consult the Magic 8 Ball")
    @TaggedResponse
    private fun eightBall(event: IEventWrapper): String {
        plugin.logDebug("user ${event.senderName} consults the Magic 8-Ball")
        val responses = plugin.eightball.data.getStringList("responses")
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
    @BotCommand(name = "choose", usage = "<option>, <option>, ... (delimiters include ',', 'or', '|')",
            description = "Have the bot choose between given options")
    @TaggedResponse
    private fun choose(event: IEventWrapper, query: String): String {
        plugin.logDebug("user ${event.senderName} needs a choice made")
        val choices = query.split(", or ", " or ", ",", "|")
        val rand = Random().nextInt(choices.count())
        return "I pick '${choices[rand]}'"
    }

    /**
     * Press F to pay respects
     *
     * @param event the incoming event object
     * @return the response string
     */
    @BotCommand(usage="", description = "Press F to pay respects")
    private fun f(event: IEventWrapper): String {
        plugin.logDebug("user ${event.senderName}} pays respects")
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
        val msg: String
        if (found.message.contains("%u"))
            msg = found.message.replace("%u", event.senderAsMention).replace("%t", totalRespects.toString())
                .replace("%c", found.count.toString())
        else
            msg = "${event.senderAsMention} | ${found.message}".replace("%t", totalRespects.toString())
                    .replace("%c", found.count.toString())

        plugin.f.data.set("total-respects", totalRespects)
        plugin.f.saveConfig()

        return msg
    }

    /**
     * Insult someone or something
     *
     * @param event the incoming event object
     * @param thingToInsult the target of the insult
     * @return the response string
     */
    @BotCommand(usage="<thing to insult>", description = "Make the bot insult something for you")
    private fun insult(event: IEventWrapper, thingToInsult: String): String {
        plugin.logDebug("user ${event.senderName} requests an insult against '$thingToInsult'")
        val responses = plugin.insult.data.getStringList("responses")
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
    @BotCommand(usage="<thing to be rated>", description = "Have the bot rate something for you")
    @TaggedResponse
    private fun rate(event: IEventWrapper, thingToRate: String): String {
        plugin.logDebug("user ${event.senderName} requests a rating")
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

        var thingToBeRated = thingToRate
        if (plugin.rate.data.getBoolean("translate-first-and-second-person", true)) {
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
    @BotCommand(name = "roll", usage = "<sides>", description = "Roll a die for a random number")
    @TaggedResponse
    private fun roll(event: IEventWrapper, sides: Int): String {
        plugin.logDebug("user ${event.senderName} needs a choice made")
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
    @BotCommand(usage="<say something>", description = "Say something to Cleverbot")
    private fun talk(event: IEventWrapper, query: String): String {
        plugin.logDebug("user ${event.senderName} invokes Cleverbot")

        if (Config.CLEVERBOT_KEY.isEmpty())
            return "You do not have an API key. Go to https://www.cleverbot.com/JDA/ for more information."
        val bot: CleverBotQuery = CleverBotQuery(Config.CLEVERBOT_KEY, query)
        bot.sendRequest()
        return bot.response
    }

    /**
     * Shortcut method for adding "or <prefix>help " to the CommandNotFound output if a COMMAND_PREFIX is set in config
     */
    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Any> List<*>.checkItemsAre() = if (all { it is T }) this as List<T> else null
}
