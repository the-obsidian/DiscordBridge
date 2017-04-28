package gg.obsidian.discordbridge.commands

import gg.obsidian.discordbridge.Config
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel

class MessageWrapper(val originalMessage: Message) : IEventWrapper {

    override val senderAsMention: String
        get() = originalMessage.author.asMention

    override val channel: MessageChannel
        get() = originalMessage.channel

    override val isFromRelayChannel: Boolean
        get() = if (originalMessage.isFromType(ChannelType.PRIVATE)) false
                else originalMessage.guild.id == Config.SERVER_ID
                && originalMessage.isFromType(ChannelType.TEXT)
                && originalMessage.textChannel.name.equals(Config.CHANNEL, true)

    override val message: String
        get() = originalMessage.content

    override val rawMessage: String
        get() = originalMessage.rawContent

    override val senderName: String
        get() = originalMessage.author.name

    override val senderId: String
        get() = originalMessage.author.id

}
