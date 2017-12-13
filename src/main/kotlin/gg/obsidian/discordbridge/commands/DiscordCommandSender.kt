package gg.obsidian.discordbridge.commands

import net.dv8tion.jda.core.entities.MessageChannel
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.command.RemoteConsoleCommandSender
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionAttachment
import org.bukkit.permissions.PermissionAttachmentInfo
import org.bukkit.plugin.Plugin

class DiscordCommandSender(val channel: MessageChannel) : RemoteConsoleCommandSender {

    private val sender:ConsoleCommandSender = Bukkit.getServer().consoleSender

    init {

    }

    override fun sendMessage(message: String?) {
        channel.sendMessage(message).queue()
    }

    override fun sendMessage(messages: Array<out String>?) {
        if (messages != null)
            for (m in messages) channel.sendMessage(m)
    }

    override fun spigot(): CommandSender.Spigot {
        return sender.spigot()
    }

    override fun addAttachment(plugin: Plugin?): PermissionAttachment {
        return sender.addAttachment(plugin)
    }

    override fun addAttachment(plugin: Plugin?, ticks: Int): PermissionAttachment {
        return sender.addAttachment(plugin, ticks)
    }

    override fun addAttachment(plugin: Plugin?, name: String?, value: Boolean): PermissionAttachment {
        return sender.addAttachment(plugin, name, value)
    }

    override fun addAttachment(plugin: Plugin?, name: String?, value: Boolean, ticks: Int): PermissionAttachment {
        return sender.addAttachment(plugin, name, value, ticks)
    }

    override fun getEffectivePermissions(): MutableSet<PermissionAttachmentInfo> {
        return sender.effectivePermissions
    }

    override fun getName(): String {
        return sender.name
    }

    override fun getServer(): Server {
        return sender.server
    }

    override fun hasPermission(name: String?): Boolean {
        return sender.hasPermission(name)
    }

    override fun hasPermission(perm: Permission?): Boolean {
        return sender.hasPermission(perm)
    }

    override fun isOp(): Boolean {
        return sender.isOp
    }

    override fun isPermissionSet(name: String?): Boolean {
        return sender.isPermissionSet(name)
    }

    override fun isPermissionSet(perm: Permission?): Boolean {
        return sender.isPermissionSet(perm)
    }

    override fun recalculatePermissions() {
        return sender.recalculatePermissions()
    }

    override fun removeAttachment(attachment: PermissionAttachment?) {
        return sender.removeAttachment(attachment)
    }

    override fun setOp(value: Boolean) {
        return sender.setOp(value)
    }


}