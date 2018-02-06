package gg.obsidian.discordbridge.wrapper

import org.spongepowered.api.world.World

class DbSpongeWorld(private val spongeWorld: World) : IDbWorld {
    override fun getName(): String {
        return spongeWorld.name
    }

}