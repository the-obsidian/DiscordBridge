package gg.obsidian.discordbridge.wrappers

interface IScheduler {
    fun runAsyncTask(task: Runnable)
}