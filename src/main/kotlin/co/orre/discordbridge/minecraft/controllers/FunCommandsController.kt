package co.orre.discordbridge.minecraft.controllers

import co.orre.discordbridge.CommandLogic
import co.orre.discordbridge.Config
import co.orre.discordbridge.Plugin
import co.orre.discordbridge.discord.Connection
import co.orre.discordbridge.minecraft.Permissions
import co.orre.discordbridge.minecraft.interfaces.BotCommand
import co.orre.discordbridge.minecraft.interfaces.BotController
import co.orre.discordbridge.utils.UtilFunctions.toMinecraftChatMessage
import net.dv8tion.jda.core.entities.Message
import org.bukkit.event.player.AsyncPlayerChatEvent

class FunCommandsController(val plugin: Plugin) : BotController {

    override fun getDescription(): String = ":balloon:  **FUN** - Every bot has to have them!"

    // 8BALL - consult the Magic 8-Ball to answer your yes or no questions
    @BotCommand(name = "8ball", usage = "<question>", description = "Consult the Magic 8 Ball")
    private fun eightBall(event: AsyncPlayerChatEvent){
        if (!Permissions.eightball.has(event.player))
            return
        plugin.logDebug("player ${event.player.name} consults the Magic 8-Ball")
        dispatchResponse(event, CommandLogic.eightBall(plugin, event.player.name))
    }

    // CHOOSE - chooses between some number of options
    @BotCommand(name = "choose", usage = "<option>, <option>, ... (delimiters include ',', 'or', '|')",
            description = "Have the bot choose between given options")
    private fun choose(event: AsyncPlayerChatEvent, query: String){
        plugin.logDebug("user ${event.player.name} needs a choice made")
        dispatchResponse(event, CommandLogic.choose(event.player.name, query))
    }

    // F - press F to pay respects!
    @BotCommand(usage="", description = "Press F to pay respects")
    private fun f(event: AsyncPlayerChatEvent) {
        plugin.logDebug("user ${event.player.name} pays respects")
        dispatchResponse(event, CommandLogic.f(plugin, event.player.name))
    }

    // INSULT - the bot insults something
    @BotCommand(usage="<thing to insult>", description = "Make the bot insult something for you")
    private fun insult(event: AsyncPlayerChatEvent, thingToInsult: String) {
        plugin.logDebug("user ${event.player.name} requests an insult against '$thingToInsult'")
        dispatchResponse(event, CommandLogic.insult(plugin, event.player.name, thingToInsult))
    }

    // RATE - the bot rates something
    @BotCommand(usage="<thing to be rated>", description = "Have the bot rate something for you")
    private fun rate(event: AsyncPlayerChatEvent, thingToRate: String) {
        plugin.logDebug("user ${event.player.name} requests a rating")
        dispatchResponse(event, CommandLogic.rate(plugin, event.player.name, thingToRate))
    }

    // ROLL - roll a die for a random number
    @BotCommand(name = "roll", usage = "<sides>", description = "Roll a die for a random number")
    private fun roll(event: AsyncPlayerChatEvent, sides: Int){
        plugin.logDebug("user ${event.player.name} needs a choice made")
        dispatchResponse(event, CommandLogic.roll(event.player.name, sides))
    }

    // TALK - directly speak to Cleverbot (useful for not accidentally invoking other commands)
    @BotCommand(usage="<say something>", description = "Say something to Cleverbot")
    private fun talk(event: AsyncPlayerChatEvent, query: String) {
        plugin.logDebug("user ${event.player.name} invokes Cleverbot")
        dispatchResponse(event, CommandLogic.askCleverbot(event.player.name, query))
    }

    private fun dispatchResponse(event: AsyncPlayerChatEvent, response: String) {
        plugin.sendToMinecraft(response.toMinecraftChatMessage(Config.BOT_MC_USERNAME))

        var response1 = plugin.convertAtMentions(response)

        plugin.sendToDiscord(response1, Connection.getRelayChannel())
    }

    private fun Message.isFromRelayChannel(): Boolean = guild.id == Config.SERVER_ID
            && this.isFromType(net.dv8tion.jda.core.entities.ChannelType.TEXT)
            && this.textChannel.name.equals(Config.CHANNEL, true)
}
