package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.commands.Command
import gg.obsidian.discordbridge.wrappers.DbSpongePlayer
import org.spongepowered.api.command.CommandCallable
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.entity.living.player.Player as SpongePlayer
import org.spongepowered.api.text.Text
import org.spongepowered.api.world.Location
import org.spongepowered.api.world.World
import java.util.*

class CommandWrapper(private val command: Command): CommandCallable {
    override fun process(source: CommandSource, arguments: String): CommandResult {
        if (source is SpongePlayer) {
            DiscordBridge.handleCommand(DbSpongePlayer(source), command.aliases[0], arguments.split(" ").toTypedArray())
            return CommandResult.success()
        }
        return CommandResult.empty()
    }

    override fun getSuggestions(source: CommandSource?, arguments: String?, targetPosition: Location<World>?): MutableList<String> {
        // TODO
        return mutableListOf()
    }

    override fun getUsage(source: CommandSource?): Text {
        return Text.of(command.usage)
    }

    override fun testPermission(source: CommandSource?): Boolean {
        // TODO
        return true
    }

    override fun getShortDescription(source: CommandSource?): Optional<Text> {
        return Optional.of(Text.of(command.description))
    }

    override fun getHelp(source: CommandSource?): Optional<Text> {
        // Todo
        return Optional.of(Text.of(command.usage))
    }
}