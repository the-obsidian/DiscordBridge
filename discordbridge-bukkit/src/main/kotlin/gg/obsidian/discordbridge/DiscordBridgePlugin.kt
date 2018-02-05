package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.wrappers.Server
import org.bukkit.plugin.java.JavaPlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * The primary Plugin class that maintains the plugin's connection with Bukkit
 */
class DiscordBridgePlugin : JavaPlugin() {

    private lateinit var instance: DiscordBridgePlugin
    private lateinit var logger: Logger

    fun getPlugin() : DiscordBridgePlugin = instance

    fun getSLF4JLogger(): Logger = logger

    override fun onLoad() {
        logger = LoggerFactory.getLogger("DiscordBrdige")
    }

    override fun onEnable() {
        instance = this
        DiscordBridge.init(Server(this, this.server), dataFolder)

        server.pluginManager.registerEvents(EventListener(), this)

        getCommand("discord").executor = EventListener()
        getCommand("f").executor = EventListener()
        getCommand("rate").executor = EventListener()
        getCommand("8ball").executor = EventListener()
        getCommand("insult").executor = EventListener()
        getCommand("choose").executor = EventListener()
        getCommand("talk").executor = EventListener()
        getCommand("roll").executor = EventListener()

        DiscordBridge.handleServerStart()
    }

    override fun onDisable() {
        DiscordBridge.handleServerStop()
    }

}
