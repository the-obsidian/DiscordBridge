package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.wrapper.DbForgeServer
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.event.FMLServerStartedEvent
import net.minecraftforge.fml.common.event.FMLServerStartingEvent
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent
import org.apache.logging.log4j.Logger
import java.io.File

@Mod(modid = "discordbridge-obsidian", useMetadata = true, serverSideOnly = true, acceptableRemoteVersions = "*")
class ForgeDiscordBridge {
    lateinit var logger: Logger

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        logger = event.modLog
        val workingDir = File(event.modConfigurationDirectory.toString() + File.separator + "discordbridge")
        DiscordBridge.init(DbForgeServer(this), workingDir)
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
}
