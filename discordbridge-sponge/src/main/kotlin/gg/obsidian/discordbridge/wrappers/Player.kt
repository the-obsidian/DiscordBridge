package gg.obsidian.discordbridge.wrappers

import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.text.Text
import java.util.*

class Player(private val spongePlayer: Player) : IPlayer {
    override fun getWorld(): IWorld {
        return World(spongePlayer.world)
    }

    override fun isVanished(): Boolean {
        return spongePlayer.get(Keys.INVISIBLE).orElse(false)
    }

    override fun getName(): String {
        return spongePlayer.name
    }

    override fun getUUID(): UUID {
        return spongePlayer.uniqueId
    }

    override fun sendMessage(message: String) {
        spongePlayer.sendMessage(Text.of(message))
    }

    override fun hasPermission(permission: String): Boolean {
        return spongePlayer.hasPermission(permission)
    }

}