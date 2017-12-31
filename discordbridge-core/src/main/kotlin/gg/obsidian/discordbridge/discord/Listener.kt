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
 *
 * @param plugin a reference to the base Plugin object
 */
class Listener(val db: DiscordBridge) : ListenerAdapter() {

    private val controllerManager = BotControllerManager(db)

    init {
        controllerManager.registerController(FunCommandsController(db), discordExclusive = true, chatExclusive = true)
        controllerManager.registerController(UtilCommandsController(db), discordExclusive = true, chatExclusive = true)
    }

    /**
     * Callback for captured messages
     *
     * @param event the MessageReceivedEvent object
     */
    override fun onMessageReceived(event: MessageReceivedEvent) {
        db.logDebug("Received message ${event.message.id} from Discord - ${event.message.rawContent}")

        // Immediately throw out messages sent from itself
        if (event.author.id == Connection.JDA.selfUser.id) {
            db.logDebug("Ignoring message ${event.message.id} from Discord: it matches this bot's username")
            return
        }

        controllerManager.dispatchMessage(DiscordMessageWrapper(db, event.message))
    }

}