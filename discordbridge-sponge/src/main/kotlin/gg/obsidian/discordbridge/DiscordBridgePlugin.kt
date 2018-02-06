package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.commands.Command
import gg.obsidian.discordbridge.util.unwrap
import gg.obsidian.discordbridge.wrappers.CommandWrapper
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
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.event.game.state.GameStartedServerEvent
import org.spongepowered.api.event.game.state.GameStartingServerEvent
import org.spongepowered.api.event.game.state.GameStoppingServerEvent
import org.spongepowered.api.event.message.MessageEvent
import org.spongepowered.api.text.Text
import javax.inject.Inject

@Plugin(id = "discordbridge-obsidian", name = "DiscordBridge", version = "@VERSION")
class DiscordBridgePlugin {

    @Inject private lateinit var game: Game
    @Inject @DefaultConfig(sharedRoot = false) private lateinit var defaultConfig: File

    @Inject private lateinit var logger: Logger

    private lateinit var instance: DiscordBridgePlugin

    fun getPlugin() : DiscordBridgePlugin = instance

    fun getLogger() : Logger = logger

    @Listener
    fun onGameInitialization(event: GameInitializationEvent) {
        instance = this
        DiscordBridge.init(Server(this, game), defaultConfig)

        val mgr = Sponge.getCommandManager()
        for (c in DiscordBridge.getServerCommands()) {
            val wrap = CommandWrapper(c)
            mgr.register(this, wrap, *c.aliases)
        }
    }

    @Listener
    fun onServerStarted(event: GameStartedServerEvent) {
        DiscordBridge.handleServerStart()
    }

    @Listener
    fun onServerStopping(event: GameStoppingServerEvent) {
        DiscordBridge.handleServerStop()
    }

    @Listener(order = Order.LAST)
    fun onPlayerJoin(event: ClientConnectionEvent.Join) {
        DiscordBridge.handlePlayerJoin(Player(event.targetEntity))
    }

    @Listener(order = Order.LAST)
    fun onPlayerQuit(event: ClientConnectionEvent.Disconnect) {
        DiscordBridge.handlePlayerQuit(Player(event.targetEntity))
    }

    @Listener(order = Order.LAST)
    fun onPlayerDeath(event: DestructEntityEvent.Death) {
        val e = event.targetEntity
        if (e is org.spongepowered.api.entity.living.player.Player) DiscordBridge.handlePlayerDeath(Player(e), event.message.toPlain())

    }

    @Listener(order = Order.LAST)
    fun onPlayerChat(event: MessageChannelEvent.Chat) {
        val p = event.cause.first(org.spongepowered.api.entity.living.player.Player::class.java).unwrap()
        if (p != null) {
            val msg = DiscordBridge.handlePlayerChat(Player(p), event.rawMessage.toPlain(), event.isCancelled)
            event.setMessage(event.formatter.header.format(), Text.of(msg), event.formatter.footer.format())
        }
    }


}