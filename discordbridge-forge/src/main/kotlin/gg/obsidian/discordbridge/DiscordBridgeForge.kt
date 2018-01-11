package gg.obsidian.discordbridge

import net.minecraft.init.Blocks
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent

@Mod(modid = DiscordBridgeForge.MODID, version = "@VERSION@", acceptableRemoteVersions = "*")
class DiscordBridgeForge {

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        // some example code
        System.out.println("DIRT BLOCK >> " + Blocks.DIRT.unlocalizedName)
    }

    companion object {
        const val MODID = "discordbridge-obsidian"
        val VERSION = "1.0"
    }
}