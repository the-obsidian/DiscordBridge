package co.orre.discordbridge.utils

import co.orre.discordbridge.Config
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
    fun String.toDiscordChatMessage(username: String, displayName: String, worldName: String): String {
        return formatMessage(Config.TEMPLATES_DISCORD_CHAT_MESSAGE, msg = this, u = username, d = displayName, w = worldName)
    }

    @JvmStatic
    fun Player.toDiscordPlayerJoin(): String {
        return formatMessage(Config.TEMPLATES_DISCORD_PLAYER_JOIN, u = this.name.stripColor(),
                d = this.displayName.stripColor())
    }

    @JvmStatic
    fun Player.toDiscordPlayerLeave(): String {
        return formatMessage(Config.TEMPLATES_DISCORD_PLAYER_LEAVE, u = this.name.stripColor(),
                d = this.displayName.stripColor())
    }

    @JvmStatic
    fun String.toDiscordPlayerDeath(username: String, displayName: String): String {
        return formatMessage(Config.TEMPLATES_DISCORD_CHAT_MESSAGE, msg = this, u = username, d = displayName)
    }

    @JvmStatic
    private fun formatMessage(template: String, msg: String = "N/A", u: String = "N/A", d: String = "N/A",
                              w: String = "N/A"): String {
        var out = org.bukkit.ChatColor.translateAlternateColorCodes('&', template)
        out = out.replace("%u", u).replace("%m", msg).replace("%d", d).replace("%w", w)
        return out
    }
}