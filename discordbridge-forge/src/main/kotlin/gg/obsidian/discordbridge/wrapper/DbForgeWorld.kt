package gg.obsidian.discordbridge.wrapper

import net.minecraft.world.World

class DbForgeWorld(private val minecraftWorld: World) : IDbWorld {
    override fun getName(): String {
        return minecraftWorld.worldInfo.worldName
    }
}
