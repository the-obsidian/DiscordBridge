package gg.obsidian.discordbridge.wrappers

interface IDbPlayer : IDbCommandSender {
    fun getWorld(): IDbWorld
    fun isVanished(): Boolean
}