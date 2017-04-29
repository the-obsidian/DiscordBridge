package gg.obsidian.discordbridge.utils

import gg.obsidian.discordbridge.Config
import org.bukkit.ChatColor
import org.bukkit.entity.Player

/**
 * Various utility functions
 */
object UtilFunctions {

    /**
     * @return the input string with all whitespace removed
     */
    @JvmStatic
    fun String.noSpace() = this.replace(Regex("""\s+"""), "")

    /**
     * @return the input string with any Minecraft formatting codes removed
     */
    @JvmStatic
    fun String.stripColor(): String = ChatColor.stripColor(this)

    /**
     * Converts a string to the Minecraft chat message template defined in config.yml
     *
     * @param alias the name of the message author
     * @return the formatted string
     */
    @JvmStatic
    fun String.toMinecraftChatMessage(alias: String): String {
        return formatMessage(Config.TEMPLATES_MINECRAFT_CHAT_MESSAGE, msg = this, u = alias, w = "Discord")
    }

    /**
     * Converts a string to the Discord chat message template defined in config.yml
     *
     * @param username the username of the message author
     * @param worldName the name of the world the author was in when the message was sent
     * @return the formatted string
     */
    @JvmStatic
    fun String.toDiscordChatMessage(username: String, worldName: String): String {
        return formatMessage(Config.TEMPLATES_DISCORD_CHAT_MESSAGE, msg = this, u = username, w = worldName)
    }

    /**
     * Converts a string to the Discord player join message template defined in config.yml
     *
     * @param worldName the name of the world where the player joined
     * @return the formatted string
     */
    @JvmStatic
    fun Player.toDiscordPlayerJoin(worldName: String): String {
        return formatMessage(Config.TEMPLATES_DISCORD_PLAYER_JOIN, u = this.name.stripColor(), w = worldName)
    }

    /**
     * Converts a string to the Discord player leave message template defined in config.yml
     *
     * @param worldName the name of the world the player has left
     * @return the formatted string
     */
    @JvmStatic
    fun Player.toDiscordPlayerLeave(worldName: String): String {
        return formatMessage(Config.TEMPLATES_DISCORD_PLAYER_LEAVE, u = this.name.stripColor(), w = worldName)
    }

    /**
     * Converts a string to the Discord player death message template defined in config.yml
     *
     * @param username the name of the player that died
     * @param worldName the world where the player died
     * @return the formatted string
     */
    @JvmStatic
    fun String.toDiscordPlayerDeath(username: String, worldName: String): String {
        return formatMessage(Config.TEMPLATES_DISCORD_CHAT_MESSAGE, msg = this, u = username, w = worldName)
    }

    /**
     * Performs all the replacements for the "to" conversion functions
     *
     * @param template the template string
     * @param msg the raw message
     * @param u the name of the author or target player
     * @param w the world name
     */
    @JvmStatic
    private fun formatMessage(template: String, msg: String = "N/A", u: String = "N/A", w: String = "N/A"): String {
        var out = org.bukkit.ChatColor.translateAlternateColorCodes('&', template)
        out = out.replace("%u", u).replace("%m", msg).replace("%w", w)
        return out
    }
}