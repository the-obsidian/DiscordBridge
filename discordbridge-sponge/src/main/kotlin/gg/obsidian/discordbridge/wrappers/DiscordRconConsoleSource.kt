package gg.obsidian.discordbridge.wrappers

import gg.obsidian.discordbridge.commands.DiscordCommandSender
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.source.ConsoleSource
import org.spongepowered.api.service.context.Context
import org.spongepowered.api.service.permission.Subject
import org.spongepowered.api.service.permission.SubjectCollection
import org.spongepowered.api.service.permission.SubjectData
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.channel.MessageChannel
import org.spongepowered.api.util.Tristate
import java.util.*

class DiscordRconConsoleSource(private val sender: DiscordCommandSender, private val base: ConsoleSource): ConsoleSource {
    override fun sendMessage(message: Text) {
        sender.sendMessage(message.toPlain())
    }

    override fun setMessageChannel(channel: MessageChannel) {
        base.messageChannel = channel
    }

    override fun getIdentifier(): String {
        return base.identifier
    }

    override fun getMessageChannel(): MessageChannel {
        return base.messageChannel
    }

    override fun getCommandSource(): Optional<CommandSource> {
        return base.commandSource
    }

    override fun getOption(contexts: MutableSet<Context>, key: String): Optional<String> {
        return base.getOption(contexts, key)
    }

    override fun getName(): String {
        return sender.senderName
    }

    override fun getTransientSubjectData(): SubjectData {
        return base.transientSubjectData
    }

    override fun getParents(contexts: MutableSet<Context>?): MutableList<Subject> {
        return base.parents
    }

    override fun getContainingCollection(): SubjectCollection {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSubjectData(): SubjectData {
        return base.subjectData
    }

    override fun isChildOf(contexts: MutableSet<Context>, parent: Subject): Boolean {
        return base.isChildOf(contexts, parent)
    }

    override fun getActiveContexts(): MutableSet<Context> {
        return base.activeContexts
    }

    override fun getPermissionValue(contexts: MutableSet<Context>, permission: String): Tristate {
        return base.getPermissionValue(contexts, permission)
    }

}