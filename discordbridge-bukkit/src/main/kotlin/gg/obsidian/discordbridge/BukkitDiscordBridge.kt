package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.wrapper.DbBukkitServer
import org.bukkit.plugin.java.JavaPlugin

/**
 * The primary Plugin class that maintains the plugin's connection with Bukkit
 */
class BukkitDiscordBridge : JavaPlugin() {
    private lateinit var instance: BukkitDiscordBridge

    override fun onLoad() {
        server.logger.info("Loading DiscordBridge")
    }

    override fun onEnable() {
        server.logger.info("Enabling DiscordBridge")
        instance = this
        val isMultiverse = server.pluginManager.getPlugin("Multiverse-Core") != null
        DiscordBridge.init(DbBukkitServer(this, this.server), dataFolder, isMultiverse=isMultiverse)

        server.pluginManager.registerEvents(MainEventListener(), this)

        if (server.pluginManager.getPlugin("Dynmap") != null) {
            server.pluginManager.registerEvents(DynmapEventListener(), this)
        }

        getCommand("discord")?.setExecutor(MainEventListener())
        getCommand("f")?.setExecutor(MainEventListener())
        getCommand("rate")?.setExecutor(MainEventListener())
        getCommand("8ball")?.setExecutor(MainEventListener())
        getCommand("insult")?.setExecutor(MainEventListener())
        getCommand("choose")?.setExecutor(MainEventListener())
        getCommand("talk")?.setExecutor(MainEventListener())
        getCommand("roll")?.setExecutor(MainEventListener())

        DiscordBridge.handleServerStart()
    }

    override fun onDisable() {
        DiscordBridge.handleServerStop()
    }
}
