package gg.obsidian.discordbridge.commands

import net.dv8tion.jda.core.entities.MessageChannel

/**
 * Interface for wrappers of various message event types
 */
interface IEventWrapper {
    /**
     * The name of the event author
     */
    val senderName : String
    /**
     * The message in the event
     *
     * For DiscordMessageWrapper instances, this calls getContent()
     *
     * Otherwise, this is identical to rawMessage
     */
    val message : String
    /**
     * The raw message in the event
     *
     * For DiscordMessageWrapper instances, this calls getRawContent()
     *
     * Otherwise, this is identical to message
     */
    val rawMessage : String
    /**
     * The name of the author of the event in @tag format
     *
     * For DiscordMessageWrapper instances, this will return a mention tag in the form <@##########>
     *
     * Otherwise, this will return the player's username prefixed with '@'
     */
    val senderAsMention : String
    /**
     * The originating channel of the message
     *
     * For DiscordMessageWrapper instances, this returns the origin channel or private channel of the message
     *
     * Otherwise, this returns Connection.getRelayChannel()
     * @see gg.obsidian.discordbridge.discord.Connection.getRelayChannel
     */
    val channel : MessageChannel
    /**
     * The ID string of the message author
     *
     * For DiscordMessageWrapper instances, this returns the author's Discord ID
     *
     * Otherwise, this returns the author's Minecraft UUID
     */
    val senderId : String
    /**
     * Whether this message is from the channel that is relayed to Minecraft
     *
     * For DiscordMessageWrapper instances, this is true if the inner event's getChannel() is equal to
     * Connection.getRelayChannel(), and false otherwise
     * @see gg.obsidian.discordbridge.discord.Connection.getRelayChannel
     *
     * Otherwise, this always returns true
     */
    val isFromRelayChannel: Boolean
}
