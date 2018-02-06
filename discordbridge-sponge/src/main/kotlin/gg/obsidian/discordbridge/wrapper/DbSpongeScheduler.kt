package gg.obsidian.discordbridge.wrapper

import gg.obsidian.discordbridge.SpongeDiscordBridge
import org.spongepowered.api.Game

class DbSpongeScheduler(val plugin: SpongeDiscordBridge, val game: Game) : IDbScheduler {
    override fun runAsyncTask(task: Runnable) {
        game.scheduler.createTaskBuilder().execute(task).submit(plugin)
    }

}