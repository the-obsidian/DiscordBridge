package gg.obsidian.discordbridge.wrappers

import gg.obsidian.discordbridge.BukkitDiscordBridge
import org.bukkit.scheduler.BukkitScheduler

class DbBukkitScheduler(val plugin: BukkitDiscordBridge, val bukkitScheduler: BukkitScheduler) : IDbScheduler {

    override fun runAsyncTask(task: Runnable) {
        Thread(task).start()
        //bukkitScheduler.runTaskAsynchronously(plugin, task)
    }

}