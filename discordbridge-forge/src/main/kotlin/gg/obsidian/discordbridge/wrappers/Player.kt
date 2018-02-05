package gg.obsidian.discordbridge.wrappers

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.text.TextComponentString
import java.util.*

class Player(private val minecraftPlayer: EntityPlayer) : IPlayer {
    override fun getWorld(): IWorld {
        return World(minecraftPlayer.world)
    }

    override fun isVanished(): Boolean {
        //return minecraftPlayer.get(Keys.INVISIBLE).orElse(false)
        //TODO
        return false
    }

    override fun getName(): String {
        return minecraftPlayer.name
    }

    override fun getUUID(): UUID {
        return minecraftPlayer.uniqueID
    }

    override fun sendMessage(message: String) {
        minecraftPlayer.sendMessage(TextComponentString(message))
    }

    override fun hasPermission(permission: String): Boolean {
        //return minecraftPlayer.hasPermission(permission)
        // TODO
        return true
    }

}