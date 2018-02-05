package gg.obsidian.discordbridge.wrappers

import gg.obsidian.discordbridge.DiscordBridge
import gg.obsidian.discordbridge.commands.controllers.BotControllerManager
import org.spongepowered.api.command.CommandCallable
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.text.Text
import org.spongepowered.api.world.Location
import org.spongepowered.api.world.World
import java.util.*

class CommandWrapper(private val command: BotControllerManager.Command): CommandCallable {
    override fun process(source: CommandSource, arguments: String): CommandResult {
        if (source is Player) {
            DiscordBridge.handleCommand(gg.obsidian.discordbridge.wrappers.Player(source), command.name, arguments.split(" ").toTypedArray())
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