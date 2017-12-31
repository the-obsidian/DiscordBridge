package gg.obsidian.discordbridge.commands

import gg.obsidian.discordbridge.DiscordBridge
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel

/**
 * A wrapper for JDA's Message class
 *
 * @param originalMessage the underlying Message instance
 */
class DiscordMessageWrapper(val db: DiscordBridge, val originalMessage: Message) : IEventWrapper {

    /**
     * Returns a formatted mention tag in the form <@##########>
     */
    override val senderAsMention: String
        get() = originalMessage.author.asMention

    /**
     * Returns the channel in which this event was sent
     */
    override val channel: MessageChannel
        get() = originalMessage.channel

    /**
     * Whether this message was sent from the relay channel
     *
     * Returns true if the underlying message's getChannel() is equal to
     * Connection.getRelayChannel(), false otherwise
     * @see gg.obsidian.discordbridge.discord.Connection.getRelayChannel
     */
    override val isFromRelayChannel: Boolean
        get() = if (originalMessage.isFromType(ChannelType.PRIVATE)) false
                else originalMessage.guild.id == db.getConfig().getString("server-id")
                && originalMessage.isFromType(ChannelType.TEXT)
                && originalMessage.textChannel.name.equals(db.getConfig().getString("channel"), true)

    /**
     * The message of this event
     *
     * This is equivalent to Message.getContent()
     */
    override val message: String
        get() = originalMessage.content

    /**
     * The raw message of this event
     *
     * This is equivalent to Message.getRawContent()
     */
    override val rawMessage: String
        get() = originalMessage.rawContent

    /**
     * The visible server name of the author of the event
     */
    override val senderName: String
        get() = originalMessage.author.name

    /**
     * The Discord ID of the author
     */
    override val senderId: String
        get() = originalMessage.author.id

}
