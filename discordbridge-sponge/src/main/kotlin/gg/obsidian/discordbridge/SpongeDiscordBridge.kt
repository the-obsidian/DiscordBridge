package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.util.unwrap
import gg.obsidian.discordbridge.wrapper.DbSpongePlayer
import gg.obsidian.discordbridge.wrapper.DbSpongeServer
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
import org.spongepowered.api.event.game.state.GameStoppingServerEvent
import org.spongepowered.api.text.Text
import javax.inject.Inject

@Plugin(
        name = "DiscordBridge",
        id = "@MODID@",
        version = "@VERSION@"
)
class SpongeDiscordBridge {
    @Inject private lateinit var game: Game
    @Inject private lateinit var logger: Logger
    @Inject @DefaultConfig(sharedRoot = false) private lateinit var defaultConfig: File

    fun getLogger() : Logger = logger

    @Listener
    fun onGameInitialization(event: GameInitializationEvent) {
        DiscordBridge.init(DbSpongeServer(this, game), defaultConfig)

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
        DiscordBridge.handlePlayerJoin(DbSpongePlayer(event.targetEntity))
    }

    @Listener(order = Order.LAST)
    fun onPlayerQuit(event: ClientConnectionEvent.Disconnect) {
        DiscordBridge.handlePlayerQuit(DbSpongePlayer(event.targetEntity))
    }

    @Listener(order = Order.LAST)
    fun onPlayerDeath(event: DestructEntityEvent.Death) {
        val e = event.targetEntity
        if (e is org.spongepowered.api.entity.living.player.Player)
            DiscordBridge.handlePlayerDeath(DbSpongePlayer(e), event.message.toPlain())
    }

    @Listener(order = Order.LAST)
    fun onPlayerChat(event: MessageChannelEvent.Chat) {
        val p = event.cause.first(org.spongepowered.api.entity.living.player.Player::class.java).unwrap()
        if (p != null) {
            val msg = DiscordBridge.handlePlayerChat(DbSpongePlayer(p), event.rawMessage.toPlain(), event.isCancelled)
            event.setMessage(event.formatter.header.format(), Text.of(msg), event.formatter.footer.format())
        }
    }
}
