package gg.obsidian.discordbridge.wrapper

import org.bukkit.World

class DbBukkitWorld(private val bukkitWorld: World?) : IDbWorld {
    override fun getName(): String {
        return bukkitWorld?.name ?: "Unknown"
    }
}
