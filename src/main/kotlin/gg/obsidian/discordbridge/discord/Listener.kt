package gg.obsidian.discordbridge.discord

import gg.obsidian.discordbridge.Plugin
import gg.obsidian.discordbridge.commands.MessageWrapper
import gg.obsidian.discordbridge.commands.controllers.BotControllerManager
import gg.obsidian.discordbridge.commands.controllers.FunCommandsController
import gg.obsidian.discordbridge.commands.controllers.UtilCommandsController
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

/**
 * Listens for events from Discord
 *
 * @param plugin a reference to the base Plugin object
 */
class Listener(val plugin: Plugin) : ListenerAdapter() {

    private val controllerManager = BotControllerManager(plugin)

    init {
        controllerManager.registerController(FunCommandsController(plugin), discordExclusive = true, chatExclusive = true)
        controllerManager.registerController(UtilCommandsController(plugin), discordExclusive = true, chatExclusive = true)
    }

    /**
     * Callback for captured messages
     *
     * @param event the MessageReceivedEvent object
     */
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
