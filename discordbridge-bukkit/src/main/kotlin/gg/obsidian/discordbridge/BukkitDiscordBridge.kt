package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.wrappers.DbBukkitServer
import org.bukkit.plugin.java.JavaPlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * The primary Plugin class that maintains the plugin's connection with Bukkit
 */
class BukkitDiscordBridge : JavaPlugin() {

    private lateinit var instance: BukkitDiscordBridge
    private lateinit var logger: Logger

    override fun onLoad() {
        logger = LoggerFactory.getLogger("DiscordBrdige")
        logger.info("Loading DiscordBridge")
    }

    override fun onEnable() {
        logger.info("Enabling DiscordBridge")
        instance = this
        val isMultiverse = server.pluginManager.getPlugin("Multiverse-Core") != null
        DiscordBridge.init(DbBukkitServer(this, this.server), dataFolder, isMultiverse=isMultiverse)

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
