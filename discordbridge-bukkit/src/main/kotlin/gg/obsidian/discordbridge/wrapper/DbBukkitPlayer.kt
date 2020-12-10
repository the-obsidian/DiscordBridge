package gg.obsidian.discordbridge.wrapper

import org.bukkit.OfflinePlayer
import java.util.*

// TODO: Add safety for when OfflinePlayer is not online
class DbBukkitPlayer(private val bukkitPlayer: OfflinePlayer) : IDbPlayer {
    override fun hasPermission(permission: String): Boolean {
        return bukkitPlayer.player?.hasPermission(permission) ?: false
    }

    override fun getWorld(): IDbWorld {
        return DbBukkitWorld(bukkitPlayer.player?.world)
    }

    override fun isVanished(): Boolean {
        return (bukkitPlayer.player?.hasMetadata("vanished") ?: false) && (bukkitPlayer.player?.getMetadata("vanished")?.get(0)?.asBoolean() ?: false)
    }

    override fun getName(): String {
        return bukkitPlayer.name ?: ""
    }

    override fun sendMessage(message: String) {
        bukkitPlayer.player?.sendMessage(message)
    }

    override fun getUUID(): UUID {
        return bukkitPlayer.uniqueId
    }
}
