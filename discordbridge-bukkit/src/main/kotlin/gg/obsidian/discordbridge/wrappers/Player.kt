package gg.obsidian.discordbridge.wrappers

import org.bukkit.entity.Player
import java.util.*

class Player(val bukkitPlayer: Player) : IPlayer {
    override fun hasPermission(permission: String): Boolean {
        return bukkitPlayer.hasPermission(permission)
    }

    override fun getWorld(): IWorld {
        return World(bukkitPlayer.world)
    }

    override fun isVanished(): Boolean {
        return bukkitPlayer.hasMetadata("vanished") && bukkitPlayer.getMetadata("vanished")[0].asBoolean()
    }

    override fun getName(): String {
        return bukkitPlayer.name
    }

    override fun sendMessage(message: String) {
        bukkitPlayer.sendMessage(message)
    }

    override fun getUUID(): UUID {
        return bukkitPlayer.uniqueId
    }

}