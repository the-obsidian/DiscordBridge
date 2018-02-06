package gg.obsidian.discordbridge.wrappers

import org.bukkit.command.Command

class DbBukkitCommand(val cmd: Command) : IDbCommand {
    override fun getName(): String {
        return cmd.name
    }

}