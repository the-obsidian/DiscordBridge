package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.wrappers.CommandWrapper
import gg.obsidian.discordbridge.wrappers.Server
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.event.FMLServerStartingEvent
import org.apache.logging.log4j.Logger
import java.io.File

@Mod(modid = DiscordBridgeForge.MODID, version = "@VERSION@", acceptableRemoteVersions = "*")
class DiscordBridgeForge {

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        logger = event.modLog
        val workingDir = File(event.modConfigurationDirectory.toString() + File.separator + "discordbridge")
        core = DiscordBridge(Server(this), workingDir)
        core.postInit()
    }

    @Mod.EventHandler
    fun serverStart(event: FMLServerStartingEvent) {
        for (c in core.getServerCommands()) {
            event.registerServerCommand(CommandWrapper(c))
        }
    }

    lateinit var logger: Logger

    private lateinit var core: DiscordBridge

    fun getCore() : DiscordBridge = core


    companion object {
        const val MODID = "discordbridge-obsidian"

        @Mod.Instance(MODID)
        lateinit var inst: DiscordBridgeForge
    }
}