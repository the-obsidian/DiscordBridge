package gg.obsidian.discordbridge.wrappers

class DbForgeScheduler : IDbScheduler {
    override fun runAsyncTask(task: Runnable) {
        Thread(task).start()
    }
}