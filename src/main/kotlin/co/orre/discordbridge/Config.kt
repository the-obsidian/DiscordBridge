package co.orre.discordbridge

import org.bukkit.ChatColor
import co.orre.discordbridge.utils.UtilFunctions.noSpace

object Config {

    var SERVER_ID: String = ""
    var CHANNEL: String = ""
    var USERNAME: String = ""
    var USERNAME_COLOR: String = ""
    var TOKEN: String = ""
    var COMMAND_PREFIX: String = ""
    var CLEVERBOT_KEY: String = ""
    var DEBUG: Boolean = false
    var RELAY_CANCELLED_MESSAGES = true
    var ANNOUNCE_SERVER_START_STOP = true

    // Toggle message types
    var MESSAGES_CHAT = true
    var MESSAGES_JOIN = true
    var MESSAGES_LEAVE = true
    var MESSAGES_DEATH = false

    // What to do if player is vanished
    var IF_VANISHED_CHAT = false
    var IF_VANISHED_JOIN = false
    var IF_VANISHED_LEAVE = false
    var IF_VANISHED_DEATH = false

    // Discord message templates
    var TEMPLATES_DISCORD_CHAT_MESSAGE = ""
    var TEMPLATES_DISCORD_PLAYER_JOIN = ""
    var TEMPLATES_DISCORD_PLAYER_LEAVE = ""
    var TEMPLATES_DISCORD_PLAYER_DEATH = ""
    var TEMPLATES_DISCORD_SERVER_START = ""
    var TEMPLATES_DISCORD_SERVER_STOP = ""

    // Minecraft message templates
    var TEMPLATES_MINECRAFT_CHAT_MESSAGE = ""

    // misc
    var BOT_MC_USERNAME = ""

    fun load(plugin: Plugin) {
        plugin.reloadConfig()

        SERVER_ID = plugin.config.getString("server-id")
        CHANNEL = plugin.config.getString("channel")
        USERNAME = plugin.config.getString("username")
        USERNAME_COLOR = plugin.config.getString("username-color")
        TOKEN = plugin.config.getString("token", "")
        COMMAND_PREFIX = plugin.config.getString("command-prefix", "")
        CLEVERBOT_KEY = plugin.config.getString("cleverbot-key", "")
        DEBUG = plugin.config.getBoolean("debug", false)
        RELAY_CANCELLED_MESSAGES = plugin.config.getBoolean("relay-cancelled-messages", true)
        ANNOUNCE_SERVER_START_STOP = plugin.config.getBoolean("announce-server-start-stop", true)

        MESSAGES_CHAT = plugin.config.getBoolean("messages.chat", true)
        MESSAGES_JOIN = plugin.config.getBoolean("messages.join", true)
        MESSAGES_LEAVE = plugin.config.getBoolean("messages.leave", true)
        MESSAGES_DEATH = plugin.config.getBoolean("messages.death", false)

        IF_VANISHED_CHAT = plugin.config.getBoolean("if-vanished.chat", false)
        IF_VANISHED_JOIN = plugin.config.getBoolean("if-vanished.join", false)
        IF_VANISHED_LEAVE = plugin.config.getBoolean("if-vanished.leave", false)
        IF_VANISHED_DEATH = plugin.config.getBoolean("if-vanished.death", false)

        TEMPLATES_DISCORD_CHAT_MESSAGE = plugin.config.getString("templates.discord.chat-message", "<**%u**> %m")
        TEMPLATES_DISCORD_PLAYER_JOIN = plugin.config.getString("templates.discord.player-join", "**%u** joined the server")
        TEMPLATES_DISCORD_PLAYER_LEAVE = plugin.config.getString("templates.discord.player-leave", "**%u** left the server")
        TEMPLATES_DISCORD_PLAYER_DEATH = plugin.config.getString("templates.discord.player-death", "%m")
        TEMPLATES_DISCORD_SERVER_START = plugin.config.getString("templates.discord.server-start", "Server started!")
        TEMPLATES_DISCORD_SERVER_STOP = plugin.config.getString("templates.discord.server_stop", "Shutting down...")

        TEMPLATES_MINECRAFT_CHAT_MESSAGE = plugin.config.getString("templates.minecraft.chat-message", "[&b&lDiscord&r]<%u> %m")

        BOT_MC_USERNAME = ChatColor.translateAlternateColorCodes('&', USERNAME_COLOR + USERNAME.noSpace() + "&r")
    }
}
