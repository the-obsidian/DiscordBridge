package co.orre.discordbridge.commands

import co.orre.discordbridge.discord.Connection
import net.dv8tion.jda.core.entities.MessageChannel
import org.bukkit.event.player.AsyncPlayerChatEvent

class AsyncPlayerChatEventWrapper(val event: AsyncPlayerChatEvent) : IEventWrapper {
    override val senderName: String
        get() = event.player.name
    override val message: String
        get() = event.message
    override val rawMessage: String
        get() = event.message
    override val senderAsMention: String
        get() = "@" + event.player.name
    override val channel: MessageChannel
        get() = Connection.getRelayChannel()!!
    override val senderId: String
        get() = event.player.uniqueId.toString()
    override val isFromRelayChannel: Boolean
        get() = true

}
