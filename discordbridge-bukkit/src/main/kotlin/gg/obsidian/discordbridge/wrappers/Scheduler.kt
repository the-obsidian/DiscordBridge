package gg.obsidian.discordbridge.wrappers

import gg.obsidian.discordbridge.DiscordBridgePlugin
import org.bukkit.scheduler.BukkitScheduler

class Scheduler(val plugin: DiscordBridgePlugin, val bukkitScheduler: BukkitScheduler) : IScheduler {

    override fun runAsyncTask(task: Runnable) {
        Thread(task).start()
        //bukkitScheduler.runTaskAsynchronously(plugin, task)
    }

}