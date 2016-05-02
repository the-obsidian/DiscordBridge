package gg.obsidian.discordbridge

class Configuration(val plugin: Plugin) {

    var SERVER_ID: String = ""
    var CHANNEL: String = ""
    var USERNAME: String = ""
    var EMAIL: String = ""
    var PASSWORD: String = ""
    var TOKEN: String = ""
    var DEBUG: Boolean = false
    var RELAY_CANCELLED_MESSAGES = true

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

    // Minecraft message templates
    var TEMPLATES_MINECRAFT_CHAT_MESSAGE = ""

    fun load() {
        plugin.reloadConfig()

        SERVER_ID = plugin.config.getString("settings.server-id")
        CHANNEL = plugin.config.getString("settings.channel")
        USERNAME = plugin.config.getString("settings.username")
        EMAIL = plugin.config.getString("settings.email", "")
        PASSWORD = plugin.config.getString("settings.password", "")
        TOKEN = plugin.config.getString("settings.token", "")
        DEBUG = plugin.config.getBoolean("settings.debug", false)
        RELAY_CANCELLED_MESSAGES = plugin.config.getBoolean("settings.relay_cancelled_messages", true)

        MESSAGES_CHAT = plugin.config.getBoolean("settings.messages.chat", true)
        MESSAGES_JOIN = plugin.config.getBoolean("settings.messages.join", true)
        MESSAGES_LEAVE = plugin.config.getBoolean("settings.messages.leave", true)
        MESSAGES_DEATH = plugin.config.getBoolean("settings.messages.death", false)

        IF_VANISHED_CHAT = plugin.config.getBoolean("settings.if_vanished.chat", false)
        IF_VANISHED_JOIN = plugin.config.getBoolean("settings.if_vanished.join", false)
        IF_VANISHED_LEAVE = plugin.config.getBoolean("settings.if_vanished.leave", false)
        IF_VANISHED_DEATH = plugin.config.getBoolean("settings.if_vanished.death", false)

        TEMPLATES_DISCORD_CHAT_MESSAGE = plugin.config.getString("settings.templates.discord.chat_message", "<%u> %m")
        TEMPLATES_DISCORD_PLAYER_JOIN = plugin.config.getString("settings.templates.discord.player_join", "%u joined the server")
        TEMPLATES_DISCORD_PLAYER_LEAVE = plugin.config.getString("settings.templates.discord.player_leave", "%u left the server")
        TEMPLATES_DISCORD_PLAYER_DEATH = plugin.config.getString("settings.templates.discord.player_death", "%r")

        TEMPLATES_MINECRAFT_CHAT_MESSAGE = plugin.config.getString("settings.templates.minecraft.chat_message", "<%u&b(discord)&r> %m")
    }
}
