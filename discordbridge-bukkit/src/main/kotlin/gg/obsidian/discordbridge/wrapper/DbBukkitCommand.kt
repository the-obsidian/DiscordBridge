package gg.obsidian.discordbridge.wrapper

import org.bukkit.command.Command

class DbBukkitCommand(val cmd: Command) : IDbCommand {
    override fun getName(): String {
        return cmd.name
    }

}