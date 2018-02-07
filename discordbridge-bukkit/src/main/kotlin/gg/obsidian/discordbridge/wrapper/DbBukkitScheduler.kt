package gg.obsidian.discordbridge.wrapper

import gg.obsidian.discordbridge.BukkitDiscordBridge
import org.bukkit.scheduler.BukkitScheduler

class DbBukkitScheduler(private val plugin: BukkitDiscordBridge, private val bukkitScheduler: BukkitScheduler) : IDbScheduler {
    override fun runAsyncTask(task: Runnable) {
        bukkitScheduler.runTaskAsynchronously(plugin, task)
    }
}
