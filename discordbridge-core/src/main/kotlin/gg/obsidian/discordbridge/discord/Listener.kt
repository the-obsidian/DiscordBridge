package gg.obsidian.discordbridge.discord

import gg.obsidian.discordbridge.DiscordBridge
import gg.obsidian.discordbridge.commands.DiscordMessageWrapper
import gg.obsidian.discordbridge.commands.controllers.BotControllerManager
import gg.obsidian.discordbridge.commands.controllers.FunCommandsController
import gg.obsidian.discordbridge.commands.controllers.UtilCommandsController
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

/**
 * Listens for events from Discord
 */
class Listener: ListenerAdapter() {

    private val controllerManager = BotControllerManager()

    init {
        controllerManager.registerController(FunCommandsController(), discordExclusive = true, chatExclusive = true)
        controllerManager.registerController(UtilCommandsController(), discordExclusive = true, chatExclusive = true)
    }

    /**
     * Callback for captured messages
     *
     * @param event the MessageReceivedEvent object
     */
    override fun onMessageReceived(event: MessageReceivedEvent) {
        DiscordBridge.logDebug("Received message ${event.message.id} from Discord - ${event.message.rawContent}")

        // Immediately throw out messages sent from itself
        if (event.author.id == Connection.JDA.selfUser.id) {
            DiscordBridge.logDebug("Ignoring message ${event.message.id} from Discord: it matches this bot's username")
            return
        }

        controllerManager.dispatchMessage(DiscordMessageWrapper(event.message))
    }

}