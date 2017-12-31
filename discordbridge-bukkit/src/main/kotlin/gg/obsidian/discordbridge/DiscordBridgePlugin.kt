package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.wrappers.Server
import org.bukkit.plugin.java.JavaPlugin

/**
 * The primary Plugin class that maintains the plugin's connection with Bukkit
 */
class DiscordBridgePlugin : JavaPlugin() {

    private lateinit var core: DiscordBridge
    private lateinit var instance: DiscordBridgePlugin

    fun getPlugin() : DiscordBridgePlugin = instance

    fun getCore() : DiscordBridge = core

    override fun onEnable() {
        instance = this
        core = DiscordBridge(Server(this, this.server), dataFolder)
        core.postInit()

        server.pluginManager.registerEvents(EventListener(core), this)

        getCommand("discord").executor = EventListener(core)
        getCommand("f").executor = EventListener(core)
        getCommand("rate").executor = EventListener(core)
        getCommand("8ball").executor = EventListener(core)
        getCommand("insult").executor = EventListener(core)
        getCommand("choose").executor = EventListener(core)
        getCommand("talk").executor = EventListener(core)
        getCommand("roll").executor = EventListener(core)
    }

}
