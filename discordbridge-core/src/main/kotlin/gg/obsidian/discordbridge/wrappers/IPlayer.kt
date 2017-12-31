package gg.obsidian.discordbridge.wrappers

interface IPlayer : ICommandSender {
    fun getWorld(): IWorld
    fun isVanished(): Boolean
}