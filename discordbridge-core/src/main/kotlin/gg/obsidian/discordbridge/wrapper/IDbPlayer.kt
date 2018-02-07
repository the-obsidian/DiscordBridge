package gg.obsidian.discordbridge.wrapper

interface IDbPlayer : IDbCommandSender {
    fun getWorld(): IDbWorld

    fun isVanished(): Boolean
}
