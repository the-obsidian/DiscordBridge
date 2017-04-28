package gg.obsidian.discordbridge.utils

import gg.obsidian.discordbridge.Config
import org.bukkit.ChatColor
import org.bukkit.entity.Player

object UtilFunctions {

    @JvmStatic
    fun String.noSpace() = this.replace(Regex("""\s+"""), "")

    @JvmStatic
    fun String.stripColor(): String = ChatColor.stripColor(this)

    @JvmStatic
    fun String.toMinecraftChatMessage(alias: String): String {
        return formatMessage(Config.TEMPLATES_MINECRAFT_CHAT_MESSAGE, msg = this, u = alias)
    }

    @JvmStatic
    fun String.toDiscordChatMessage(username: String, worldName: String): String {
        return formatMessage(Config.TEMPLATES_DISCORD_CHAT_MESSAGE, msg = this, u = username, w = worldName)
    }

    @JvmStatic
    fun Player.toDiscordPlayerJoin(): String {
        return formatMessage(Config.TEMPLATES_DISCORD_PLAYER_JOIN, u = this.name.stripColor())
    }

    @JvmStatic
    fun Player.toDiscordPlayerLeave(): String {
        return formatMessage(Config.TEMPLATES_DISCORD_PLAYER_LEAVE, u = this.name.stripColor())
    }

    @JvmStatic
    fun String.toDiscordPlayerDeath(username: String): String {
        return formatMessage(Config.TEMPLATES_DISCORD_CHAT_MESSAGE, msg = this, u = username)
    }

    @JvmStatic
    private fun formatMessage(template: String, msg: String = "N/A", u: String = "N/A", w: String = "N/A"): String {
        var out = org.bukkit.ChatColor.translateAlternateColorCodes('&', template)
        out = out.replace("%u", u).replace("%m", msg).replace("%w", w)
        return out
    }
}