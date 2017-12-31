package gg.obsidian.discordbridge.wrappers

import gg.obsidian.discordbridge.DiscordBridgePlugin
import org.spongepowered.api.Game

class Scheduler(val plugin: DiscordBridgePlugin, val game: Game) : IScheduler {
    override fun runAsyncTask(task: Runnable) {
        game.scheduler.createTaskBuilder().execute(task).submit(plugin)
    }

}