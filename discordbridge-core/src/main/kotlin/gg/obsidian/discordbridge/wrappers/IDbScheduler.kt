package gg.obsidian.discordbridge.wrappers

interface IDbScheduler {
    fun runAsyncTask(task: Runnable)
}