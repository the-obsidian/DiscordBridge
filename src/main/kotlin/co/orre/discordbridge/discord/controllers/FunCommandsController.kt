package co.orre.discordbridge.discord.controllers

import co.orre.discordbridge.CommandLogic
import co.orre.discordbridge.Config
import co.orre.discordbridge.Plugin
import co.orre.discordbridge.discord.interfaces.BotCommand
import co.orre.discordbridge.discord.interfaces.BotController
import co.orre.discordbridge.utils.UtilFunctions.toMinecraftChatMessage
import net.dv8tion.jda.core.entities.Message

class FunCommandsController(val plugin: Plugin) : BotController {

    override fun getDescription(): String = ":balloon:  **FUN** - Every bot has to have them!"

    // 8BALL - consult the Magic 8-Ball to answer your yes or no questions
    @BotCommand(name = "8ball", usage = "<question>", description = "Consult the Magic 8 Ball")
    private fun eightBall(message: Message){
        plugin.logDebug("user ${message.author.name} consults the Magic 8-Ball")
        dispatchResponse(message, CommandLogic.eightBall(plugin, message.author.asMention))
    }

    // CHOOSE - chooses between some number of options
    @BotCommand(name = "choose", usage = "<option>, <option>, ... (delimiters include ',', 'or', '|')",
            description = "Have the bot choose between given options")
    private fun choose(message: Message, query: String){
        plugin.logDebug("user ${message.author.name} needs a choice made")
        dispatchResponse(message, CommandLogic.choose(message.author.asMention, query))
    }

    // F - press F to pay respects!
    @BotCommand(usage="", description = "Press F to pay respects")
    private fun f(message: Message) {
        plugin.logDebug("user ${message.author.name} pays respects")
        dispatchResponse(message, CommandLogic.f(plugin, message.author.asMention))
    }

    // INSULT - the bot insults something
    @BotCommand(usage="<thing to insult>", description = "Make the bot insult something for you")
    private fun insult(message: Message, thingToInsult: String) {
        plugin.logDebug("user ${message.author.name} requests an insult against '$thingToInsult'")
        dispatchResponse(message, CommandLogic.insult(plugin, message.author.name, thingToInsult))
    }

    // RATE - the bot rates something
    @BotCommand(usage="<thing to be rated>", description = "Have the bot rate something for you")
    private fun rate(message: Message, thingToRate: String) {
        plugin.logDebug("user ${message.author.name} requests a rating")
        dispatchResponse(message, CommandLogic.rate(plugin, message.author.name, thingToRate))
    }

    // ROLL - roll a die for a random number
    @BotCommand(name = "roll", usage = "<sides>", description = "Roll a die for a random number")
    private fun roll(message: Message, sides: Int){
        plugin.logDebug("user ${message.author.name} needs a choice made")
        dispatchResponse(message, CommandLogic.roll(message.author.asMention, sides))
    }

    // TALK - directly speak to Cleverbot (useful for not accidentally invoking other commands)
    @BotCommand(usage="<say something>", description = "Say something to Cleverbot")
    private fun talk(message: Message, query: String) {
        plugin.logDebug("user ${message.author.name} invokes Cleverbot")
        dispatchResponse(message, CommandLogic.askCleverbot(message.author.asMention, query))
    }

    private fun dispatchResponse(message: Message, response: String) {
        plugin.sendToDiscord(response, message.channel)

        if (message.isFromRelayChannel()) {
            var response1 = plugin.translateAliasToMinecraft(response, message.author.id)
            response1 = response1.toMinecraftChatMessage(Config.BOT_MC_USERNAME)
            plugin.sendToMinecraft(response1)
        }
    }

    private fun Message.isFromRelayChannel(): Boolean = guild.id == Config.SERVER_ID
            && this.isFromType(net.dv8tion.jda.core.entities.ChannelType.TEXT)
            && this.textChannel.name.equals(Config.CHANNEL, true)
}
