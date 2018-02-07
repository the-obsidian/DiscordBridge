package gg.obsidian.discordbridge.wrapper

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.text.TextComponentString
import java.util.*

class DbForgePlayer(private val minecraftPlayer: EntityPlayer) : IDbPlayer {
    override fun getWorld(): IDbWorld {
        return DbForgeWorld(minecraftPlayer.world)
    }

    override fun isVanished(): Boolean {
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
        // TODO
        return true
    }
}
