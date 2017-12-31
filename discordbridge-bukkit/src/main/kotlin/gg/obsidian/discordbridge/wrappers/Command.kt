package gg.obsidian.discordbridge.wrappers

import org.bukkit.command.Command

class Command(val cmd: Command) : ICommand {
    override fun getName(): String {
        return cmd.name
    }

}