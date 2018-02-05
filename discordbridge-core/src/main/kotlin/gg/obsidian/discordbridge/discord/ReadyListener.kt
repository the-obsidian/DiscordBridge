package gg.obsidian.discordbridge.discord

import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.hooks.EventListener

class ReadyListener : EventListener {
    override fun onEvent(event: Event?) {
        if (event is ReadyEvent) {

        }
    }
}