package gg.obsidian.discordbridge.commands

import gg.obsidian.discordbridge.discord.Connection
import net.dv8tion.jda.core.entities.MessageChannel
import org.bukkit.event.player.AsyncPlayerChatEvent

/**
 * A wrapper for Bukkit's AsyncPlayerChatEvent class
 *
 * @param event the underlying AsyncPlayerChatEvent instance
 */
class AsyncPlayerChatEventWrapper(val event: AsyncPlayerChatEvent) : IEventWrapper {
    /**
     * The Minecraft username of the event author
     */
    override val senderName: String
        get() = event.player.name
    /**
     * The message of this event
     */
    override val message: String
        get() = event.message
    /**
     * The raw message of this event
     *
     * This is identical to the message property for this wrapper type
     */
    override val rawMessage: String
        get() = event.message
    /**
     * The Minecraft username of the sender in "@name" format
     */
    override val senderAsMention: String
        get() = "@" + event.player.name
    /**
     * Returns the value at Connection.getRelayChannel()
     * @see Connection.getRelayChannel
     */
    override val channel: MessageChannel
        get() = Connection.getRelayChannel()!!
    /**
     * The message author's Minecraft UUID
     */
    override val senderId: String
        get() = event.player.uniqueId.toString()
    /**
     * Always returns true for this wrapper type
     */
    override val isFromRelayChannel: Boolean
        get() = true

}
