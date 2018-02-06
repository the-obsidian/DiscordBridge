package gg.obsidian.discordbridge.wrappers

import org.bukkit.World

class DbBukkitWorld(val bukkitWorld: World) : IDbWorld {
    override fun getName(): String {
        return bukkitWorld.name
    }

}