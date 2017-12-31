package gg.obsidian.discordbridge.commands

import gg.obsidian.discordbridge.discord.Connection
import gg.obsidian.discordbridge.wrappers.IPlayer
import net.dv8tion.jda.core.entities.MessageChannel

/**
 * A wrapper for Bukkit's AsyncPlayerChatEvent class
 *
 * @param event the underlying AsyncPlayerChatEvent instance
 */
class MinecraftChatEventWrapper(val player: IPlayer, val chatMessage: String) : IEventWrapper {
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
