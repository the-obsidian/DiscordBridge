package gg.obsidian.discordbridge.wrappers

import gg.obsidian.discordbridge.commands.DiscordCommandSender
import org.bukkit.Server
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.command.RemoteConsoleCommandSender
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionAttachment
import org.bukkit.permissions.PermissionAttachmentInfo
import org.bukkit.plugin.Plugin

class DiscordRConConsoleSource(val discordSender: DiscordCommandSender, val base: ConsoleCommandSender) : RemoteConsoleCommandSender {

    override fun sendMessage(message: String) {
        discordSender.sendMessage(message)
    }

    override fun sendMessage(messages: Array<out String>?) {
        if (messages != null)
            for (m in messages) discordSender.sendMessage(m)
    }

    override fun spigot(): CommandSender.Spigot {
        return base.spigot()
    }

    override fun addAttachment(plugin: Plugin?): PermissionAttachment {
        return base.addAttachment(plugin)
    }

    override fun addAttachment(plugin: Plugin?, ticks: Int): PermissionAttachment {
        return base.addAttachment(plugin, ticks)
    }

    override fun addAttachment(plugin: Plugin?, name: String?, value: Boolean): PermissionAttachment {
        return base.addAttachment(plugin, name, value)
    }

    override fun addAttachment(plugin: Plugin?, name: String?, value: Boolean, ticks: Int): PermissionAttachment {
        return base.addAttachment(plugin, name, value, ticks)
    }

    override fun getEffectivePermissions(): MutableSet<PermissionAttachmentInfo> {
        return base.effectivePermissions
    }

    override fun getName(): String {
        return discordSender.senderName
    }

    override fun getServer(): Server {
        return base.server
    }

    override fun hasPermission(name: String?): Boolean {
        return base.hasPermission(name)
    }

    override fun hasPermission(perm: Permission?): Boolean {
        return base.hasPermission(perm)
    }

    override fun isOp(): Boolean {
        return base.isOp
    }

    override fun isPermissionSet(name: String?): Boolean {
        return base.isPermissionSet(name)
    }

    override fun isPermissionSet(perm: Permission?): Boolean {
        return base.isPermissionSet(perm)
    }

    override fun recalculatePermissions() {
        return base.recalculatePermissions()
    }

    override fun removeAttachment(attachment: PermissionAttachment?) {
        return base.removeAttachment(attachment)
    }

    override fun setOp(value: Boolean) {
        return base.setOp(value)
    }


}