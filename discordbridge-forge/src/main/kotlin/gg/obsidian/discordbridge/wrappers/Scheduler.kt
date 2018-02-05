package gg.obsidian.discordbridge.wrappers

class Scheduler: IScheduler {
    override fun runAsyncTask(task: Runnable) {
        Thread(task).start()
    }
}