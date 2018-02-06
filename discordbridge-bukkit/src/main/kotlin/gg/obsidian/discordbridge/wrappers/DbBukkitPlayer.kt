package gg.obsidian.discordbridge.wrappers

import org.bukkit.OfflinePlayer
import java.util.*

// TODO: Add safety for when OfflinePlayer is not online
class DbBukkitPlayer(val bukkitPlayer: OfflinePlayer) : IDbPlayer {
    override fun hasPermission(permission: String): Boolean {
        return bukkitPlayer.player.hasPermission(permission)
    }

    override fun getWorld(): IDbWorld {
        return DbBukkitWorld(bukkitPlayer.player.world)
    }

    override fun isVanished(): Boolean {
        return bukkitPlayer.player.hasMetadata("vanished")
                && bukkitPlayer.player.getMetadata("vanished")[0].asBoolean()
    }

    override fun getName(): String {
        return bukkitPlayer.name
    }

    override fun sendMessage(message: String) {
        bukkitPlayer.player.sendMessage(message)
    }

    override fun getUUID(): UUID {
        return bukkitPlayer.uniqueId
    }

}