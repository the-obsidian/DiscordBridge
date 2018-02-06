package gg.obsidian.discordbridge.wrappers

import net.minecraft.world.World

class DbForgeWorld(private val minecraftWorld: World) : IDbWorld {
    override fun getName(): String {
        return minecraftWorld.worldInfo.worldName
    }

}