package gg.obsidian.discordbridge.command

import net.dv8tion.jda.core.entities.MessageChannel

class DiscordCommandSender(val senderName: String, private val channel: MessageChannel) {
    fun sendMessage(message: String) {
        channel.sendMessage(message)
    }

    fun sendMessage(messages: Array<out String>?) {
        if (messages != null)
            for (m in messages) channel.sendMessage(m)
    }
}
