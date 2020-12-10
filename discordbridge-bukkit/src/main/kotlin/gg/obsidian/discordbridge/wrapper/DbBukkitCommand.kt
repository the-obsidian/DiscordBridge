package gg.obsidian.discordbridge.wrapper

import org.bukkit.command.Command

class DbBukkitCommand(private val cmd: Command) : IDbCommand {
    override fun getName(): String {
        return cmd.name
    }
}
