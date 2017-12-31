package gg.obsidian.discordbridge.wrappers

import org.spongepowered.api.world.World

class World(private val spongeWorld: World) : IWorld {
    override fun getName(): String {
        return spongeWorld.name
    }

}