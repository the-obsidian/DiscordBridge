package gg.obsidian.discordbridge

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.dynmap.DynmapWebChatEvent

class DynmapEventListener: Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onDynmapChatEvent(event: DynmapWebChatEvent) {
        DiscordBridge.handleDynmapChat(event.name, event.message)
    }
}
