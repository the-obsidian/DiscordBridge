package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.wrappers.CommandWrapper
import gg.obsidian.discordbridge.wrappers.Server
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.event.FMLServerStartedEvent
import net.minecraftforge.fml.common.event.FMLServerStartingEvent
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent
import org.apache.logging.log4j.Logger
import java.io.File

@Mod(modid = DiscordBridgeForge.MODID, version = "@VERSION@", acceptableRemoteVersions = "*")
class DiscordBridgeForge {

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        logger = event.modLog
        val workingDir = File(event.modConfigurationDirectory.toString() + File.separator + "discordbridge")
        DiscordBridge.init(Server(this), workingDir)
    }

    @Mod.EventHandler
    fun serverStarting(event: FMLServerStartingEvent) {
        for (c in DiscordBridge.getServerCommands()) {
            event.registerServerCommand(CommandWrapper(c))
        }
    }

    @Mod.EventHandler
    fun serverStarted(event: FMLServerStartedEvent) {
        DiscordBridge.handleServerStart()
    }

    @Mod.EventHandler
    fun serverStop(event: FMLServerStoppingEvent) {
        DiscordBridge.handleServerStop()
    }

    lateinit var logger: Logger

    companion object {
        const val MODID = "discordbridge-obsidian"

        @Mod.Instance(MODID)
        lateinit var inst: DiscordBridgeForge
    }
}