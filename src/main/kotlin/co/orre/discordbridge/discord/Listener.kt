package co.orre.discordbridge.discord

import co.orre.discordbridge.Plugin
import co.orre.discordbridge.commands.MessageWrapper
import co.orre.discordbridge.commands.controllers.BotControllerManager
import co.orre.discordbridge.commands.controllers.FunCommandsController
import co.orre.discordbridge.commands.controllers.UtilCommandsController
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class Listener(val plugin: Plugin) : ListenerAdapter() {

    val controllerManager = BotControllerManager(plugin)

    init {
        controllerManager.registerController(FunCommandsController(plugin), discordExclusive = true, chatExclusive = true)
        controllerManager.registerController(UtilCommandsController(plugin), discordExclusive = true, chatExclusive = true)
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        plugin.logDebug("Received message ${event.message.id} from Discord - ${event.message.rawContent}")

        // Immediately throw out messages sent from itself
        if (event.author.id == Connection.JDA.selfUser.id) {
            plugin.logDebug("Ignoring message ${event.message.id} from Discord: it matches this bot's username")
            return
        }

        controllerManager.dispatchMessage(MessageWrapper(event.message))
    }

}
