package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.wrapper.DbForgePlayer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.util.text.TextComponentString
import net.minecraftforge.event.CommandEvent
import net.minecraftforge.event.ServerChatEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent

@Mod.EventBusSubscriber
object EventHandler {
    @JvmStatic
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onPlayerJoin(event: PlayerEvent.PlayerLoggedInEvent) {
        DiscordBridge.handlePlayerJoin(DbForgePlayer(event.player))
    }

    @JvmStatic
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onPlayerQuit(event: PlayerEvent.PlayerLoggedOutEvent) {
        DiscordBridge.handlePlayerQuit(DbForgePlayer(event.player))
    }

    @JvmStatic
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onPlayerDeath(event: LivingDeathEvent) {
        val e = event.entityLiving
        if (e is EntityPlayer) DiscordBridge.handlePlayerDeath(DbForgePlayer(e), e.combatTracker.deathMessage.unformattedText)
    }

    @JvmStatic
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onPlayerChat(event: ServerChatEvent) {
        val p = event.player
        if (p != null) {
            val message = DiscordBridge.handlePlayerChat(DbForgePlayer(p), event.message, event.isCanceled)
            event.component = TextComponentString(event.component.unformattedText.replace(event.message, message))
        }
    }

    // https://github.com/sk89q/WorldEdit/blob/85ef47ae0c2c02a5870b764ca4b0da0d9e01671f/worldedit-forge/src/main/java/com/sk89q/worldedit/forge/ForgeWorldEdit.java
    @JvmStatic
    @SubscribeEvent
    fun onCommand(event: CommandEvent) {
        val p = event.sender
        if (p is EntityPlayerMP) {
            if (p.world.isRemote) return
            DiscordBridge.handleCommand(DbForgePlayer(p), event.command.name, event.parameters)
        }
    }
}