package gg.obsidian.discordbridge.wrappers

import net.minecraft.world.World

class World(private val minecraftWorld: World) : IWorld {
    override fun getName(): String {
        return minecraftWorld.worldInfo.worldName
    }

}