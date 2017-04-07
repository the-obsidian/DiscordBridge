package co.orre.discordbridge.commands

import net.dv8tion.jda.core.entities.MessageChannel

interface IEventWrapper {
    val senderName : String
    val message : String
    val rawMessage : String
    val senderAsMention : String
    val channel : MessageChannel
    val senderId : String
    val type: WrapperType
    val isFromRelayChannel: Boolean
}
