package gg.obsidian.discordbridge.wrapper

interface IDbScheduler {
    fun runAsyncTask(task: Runnable)
}
