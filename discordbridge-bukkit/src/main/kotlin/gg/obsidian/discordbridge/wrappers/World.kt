package gg.obsidian.discordbridge.wrappers

import org.bukkit.World

class World(val bukkitWorld: World) : IWorld {
    override fun getName(): String {
        return "world"
    }

}