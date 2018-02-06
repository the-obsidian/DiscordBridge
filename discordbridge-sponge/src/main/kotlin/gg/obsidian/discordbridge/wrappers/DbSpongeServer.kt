package gg.obsidian.discordbridge.wrappers

import gg.obsidian.discordbridge.DiscordRcon
import gg.obsidian.discordbridge.SpongeDiscordBridge
import gg.obsidian.discordbridge.commands.DiscordCommandSender
import gg.obsidian.discordbridge.util.UrlAttachment
import gg.obsidian.discordbridge.util.unwrap
import org.spongepowered.api.Game
import org.spongepowered.api.Sponge
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.action.TextActions
import org.spongepowered.api.text.format.TextStyles
import java.net.URL
import java.util.*

class DbSpongeServer(private val plugin: SpongeDiscordBridge, private val game: Game) : IDbServer {
    override fun broadcastAttachment(att: UrlAttachment) {
        val msg = Text.builder("${att.sender} sent ").style(TextStyles.ITALIC).append(
                    Text.builder("an attachment")
                        .style(TextStyles.UNDERLINE)
                        .onClick(TextActions.openUrl(URL(att.url)))
                        .onHover(TextActions.showText(Text.of(att.hoverText)))
                        .build()
        ).build()
        Sponge.getServer().broadcastChannel.send(msg)
    }

    override fun getScheduler(): IDbScheduler {
        return DbSpongeScheduler(plugin, game)
    }

    override fun getVersion(): String {
        return game.platform.minecraftVersion.name
    }

    override fun getMinecraftShortVersion(): String {
        return game.platform.minecraftVersion.name
    }

    override fun getPlayer(uuid: UUID): IDbPlayer? {
        val p = Sponge.getServer().getPlayer(uuid).unwrap()
        return if (p != null) DbSpongePlayer(p) else null
    }

    override fun getPlayer(name: String): IDbPlayer? {
        val p = Sponge.getServer().getPlayer(name).unwrap()
        return if (p != null) DbSpongePlayer(p) else null
    }

    override fun getOnlinePlayers(): List<IDbPlayer> {
        return Sponge.getServer().onlinePlayers.map { DbSpongePlayer(it) }
    }

    override fun broadcastMessage(message: String) {
        Sponge.getServer().broadcastChannel.send(Text.of(message))
    }

    override fun dispatchCommand(sender: DiscordCommandSender, command: String) {
        val rcon = DiscordRcon(sender, game.server.console)
        Sponge.getCommandManager().process(rcon, command)
    }

    override fun getLogger(): DbSpongeLogger {
        return DbSpongeLogger(plugin.getLogger())
    }

}