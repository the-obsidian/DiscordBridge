package gg.obsidian.discordbridge.command

import gg.obsidian.discordbridge.discord.Connection
import gg.obsidian.discordbridge.wrapper.IDbPlayer
import net.dv8tion.jda.core.entities.MessageChannel

/**
 * Event wrapper for a player chat event
 *
 * @param player the player who triggered the event
 * @param chatMessage the contents of the message that was sent
 */
class MinecraftChatEventWrapper(val player: IDbPlayer, private val chatMessage: String) : IEventWrapper {
    /**
     * The Minecraft username of the event author
     */
    override val senderName: String
        get() = player.getName()
    /**
     * The message of this event
     */
    override val message: String
        get() = chatMessage
    /**
     * The raw message of this event
     *
     * This is identical to the message property for this wrapper type
     */
    override val rawMessage: String
        get() = chatMessage
    /**
     * The Minecraft username of the sender in "@name" format
     */
    override val senderAsMention: String
        get() = "@" + player.getName()
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
        get() = player.getUUID().toString()
    /**
     * Always returns true for this wrapper type
     */
    override val isFromRelayChannel: Boolean
        get() = true
}
