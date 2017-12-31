package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.util.unwrap
import gg.obsidian.discordbridge.wrappers.Player
import gg.obsidian.discordbridge.wrappers.Server
import org.spongepowered.api.Game
import org.spongepowered.api.config.DefaultConfig
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.Order
import org.spongepowered.api.event.entity.DestructEntityEvent
import org.spongepowered.api.event.game.state.GameInitializationEvent
import org.spongepowered.api.event.message.MessageChannelEvent
import org.spongepowered.api.event.network.ClientConnectionEvent
import org.spongepowered.api.plugin.Plugin
import java.io.File
import java.util.logging.Logger
import javax.inject.Inject

@Plugin(id = "discordbridge-obsidian", name = "DiscordBridge", version = "@VERSION")
class DiscordBridgePlugin {

    @Inject private lateinit var game: Game
    @Inject @DefaultConfig(sharedRoot = false) private lateinit var defaultConfig: File

    @Inject private lateinit var logger: Logger

    private lateinit var core: DiscordBridge
    private lateinit var instance: DiscordBridgePlugin

    fun getPlugin() : DiscordBridgePlugin = instance

    fun getCore() : DiscordBridge = core

    fun getLogger() : Logger = logger

    @Listener
    fun onGameInitialization(event: GameInitializationEvent) {
        instance = this
        core = DiscordBridge(Server(this, game), defaultConfig)
        core.postInit()

        //game.commandManager.register(this, )
    }

    @Listener(order = Order.LAST)
    fun onPlayerJoin(event: ClientConnectionEvent.Join) {
        core.handlePlayerJoin(Player(event.targetEntity))
    }

    @Listener(order = Order.LAST)
    fun onPlayerQuit(event: ClientConnectionEvent.Disconnect) {
        core.handlePlayerQuit(Player(event.targetEntity))
    }

    @Listener(order = Order.LAST)
    fun onPlayerDeath(event: DestructEntityEvent.Death) {
        val e = event.targetEntity
        if (e is org.spongepowered.api.entity.living.player.Player) core.handlePlayerDeath(Player(e), event.message.toPlain())

    }

    @Listener(order = Order.LAST)
    fun onPlayerChat(event: MessageChannelEvent.Chat) {
        val p = event.cause.first(org.spongepowered.api.entity.living.player.Player::class.java).unwrap()
        if (p != null) core.handlePlayerChat(Player(p), event.rawMessage.toPlain(), event.isCancelled)
    }


}