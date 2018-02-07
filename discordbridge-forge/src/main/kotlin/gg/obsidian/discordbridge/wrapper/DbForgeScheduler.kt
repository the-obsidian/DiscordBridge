package gg.obsidian.discordbridge.wrapper

class DbForgeScheduler : IDbScheduler {
    override fun runAsyncTask(task: Runnable) {
        Thread(task).start()
    }
}
