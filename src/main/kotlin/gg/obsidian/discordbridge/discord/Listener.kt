package gg.obsidian.discordbridge.discord

import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketException
import com.neovisionaries.ws.client.WebSocketFrame
import gg.obsidian.discordbridge.CommandLogic
import gg.obsidian.discordbridge.Plugin
import gg.obsidian.discordbridge.Utils.Script
import gg.obsidian.discordbridge.Utils.noSpace
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class Listener(val plugin: Plugin, val api: JDA, val connection: Connection) : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        plugin.logDebug("Received message ${event.message.id} from Discord - ${event.message.rawContent}")

        val rawmsg: String = event.message.rawContent
        val username: String = event.author.name
        val channel = event.channel
        val id = event.author.id

        // Immediately throw out messages sent from itself
        if (id == api.selfUser.id) {
            plugin.logDebug("Ignoring message ${event.message.id} from Discord: it matches this bot's username")
            return
        }

        // SCRIPTED RESPONSE - The bot replies with a pre-programmed response if it detects
        // a corresponding trigger string
        val responses = plugin.script.data.getList("responses").checkItemsAre<Script>()
        if (responses == null) plugin.logger.warning("ERROR: Responses for this command could not be read from the config.")
        else {
            val response: Script?
            for (r in responses) {
                val ignorecase = !(r.caseSensitive != null && r.caseSensitive)
                val startswith = r.startsWith != null && r.startsWith
                val requiresmention = r.requiresMention != null && r.requiresMention

                if (requiresmention) if ()
            }
        }

        // NON-TRIGGER-RELAY COMMANDS - the triggers of these commands are never relayed to Minecraft
        if (rawmsg.startsWith(api.selfUser.asMention, true)) {
            val arg = rawmsg.replaceFirst(api.selfUser.asMention, "").trimStart()

            // NON-RESPONSE-RELAY COMMANDS - the responses of these commands are also never relayed to Minecraft

            // CONFIRM - Confirm an alias
            if (arg.startsWith("confirm", true) && event.isFromType(ChannelType.PRIVATE)) {
                plugin.logDebug("user $username wants to confirm an alias")
                val ua = plugin.requests.find { it.discordId == id }
                if (ua == null) {
                    plugin.sendToDiscord("You have not requested an alias, or your request has expired!", channel)
                    return
                }
                plugin.saveAlias(ua)
                plugin.requests.remove(ua)
                plugin.sendToDiscord("Successfully linked aliases!", channel)
                return
            }

            // SERVERLIST - List all players currently online on the server
            if (arg.startsWith("serverlist", true)) {
                plugin.logDebug("user $username has requested a listing of online players")
                val players = plugin.getOnlinePlayers()
                if (players.isEmpty()) {
                    plugin.sendToDiscord("Nobody is currently online.", channel)
                    return
                }
                val response = players.joinToString("\n", "The following players are currently online:\n```\n", "\n```")
                plugin.sendToDiscord(response, channel)
                return
            }

            // RESPONSE-RELAY COMMANDS - only the responses of these commands will be relayed
            // to Minecraft if sent to the relay channel

            // 8BALL - consult the Magic 8-Ball to answer your yes or no questions
            if (arg.startsWith("8ball", true)) {
                plugin.logDebug("user $username consults the Magic 8-Ball")
                val response = CommandLogic.eightBall(plugin, username)
                plugin.sendToDiscord(response, channel)
                if (event.isFromRelayChannel())
                    plugin.sendToMinecraft(plugin.toMinecraftChatMessage(
                            plugin.translateAliasToMinecraft(response, id), plugin.cfg.BOT_MC_USERNAME))
                return
            }

            // F - press F to pay respects!
            if (arg.equals("f", true)) {
                plugin.logDebug("user $username pays respects")
                val response = CommandLogic.f(plugin, username)
                plugin.sendToDiscord(response, channel)
                if (event.isFromRelayChannel())
                    plugin.sendToMinecraft(plugin.toMinecraftChatMessage(
                            plugin.translateAliasToMinecraft(response, id), plugin.cfg.BOT_MC_USERNAME))
                return
            }

            // RATE - the bot rates something
            if (arg.startsWith("rate", true)) {
                val arg2 = arg.replaceFirst("rate", "").trimStart()
                plugin.logDebug("user $username requests a rating")
                val response = CommandLogic.rate(plugin, username, arg2)
                plugin.sendToDiscord(response, channel)
                if (event.isFromRelayChannel())
                    plugin.sendToMinecraft(plugin.toMinecraftChatMessage(
                            plugin.translateAliasToMinecraft(response, id), plugin.cfg.BOT_MC_USERNAME))
                return
            }

            // INSULT - the bot insults something
            if (arg.startsWith("insult", true)) {
                val arg2 = arg.replaceFirst("insult", "").trimStart()
                plugin.logDebug("user $username requests an insult against $arg2")
                val response = CommandLogic.insult(plugin, username, arg2)
                plugin.sendToDiscord(response, channel)
                if (event.isFromRelayChannel())
                    plugin.sendToMinecraft(plugin.toMinecraftChatMessage(
                            plugin.translateAliasToMinecraft(response, id), plugin.cfg.BOT_MC_USERNAME))
                return
            }
        }

        // If it is from the relay channel, relay it immediately
        if (event.isFromType(ChannelType.TEXT)) {

            if (!event.textChannel.name.equals(plugin.cfg.CHANNEL, true))
                plugin.logDebug("Not relaying message ${event.message.id} from Discord: channel does not match")
            else {
                plugin.logDebug("Broadcasting message ${event.message.id} from Discord to Minecraft as user $username")
                var alias = plugin.users.data.getString("discordaliases.$id.mcusername")
                if (alias == null) alias = username.noSpace()
                plugin.sendToMinecraft(plugin.toMinecraftChatMessage(event.message.content, alias))
            }
        }

        // RELAY COMMANDS - The outputs of these commands WILL be relayed to Minecraft if sent to the relay channel
        if (rawmsg.startsWith(api.selfUser.asMention, true)) {
            val arg = rawmsg.replaceFirst(api.selfUser.asMention, "").trimStart()
            if (arg.isEmpty()) return
            plugin.logDebug("Relay command received.  Arg: $arg")



//            var scripted_response: String? = null
//            for (r in responses) {
//                val casesensitive = plugin.script.data.getBoolean("responses.$r.case-sensitive", false)
//                if (arg.startsWith(plugin.script.data.getString("responses.$r.trigger").toLowerCase(), !casesensitive))
//                    scripted_response = plugin.script.data.getString("responses.$r.response")
//            }
//            if (scripted_response != null) {
//                plugin.logDebug("user $username has triggered the scripted response: $scripted_response")
//                plugin.sendToDiscord(scripted_response, channel)
//                if (event.isFromRelayChannel())
//                    plugin.sendToMinecraft(plugin.toMinecraftChatMessage(scripted_response, plugin.cfg.BOT_MC_USERNAME))
//                return
//            }

            // CLEVERBOT - Assume anything else invokes Cleverbot
            plugin.logDebug("user $username asks CleverBot something")
            val response = CommandLogic.askCleverbot(plugin, arg)
            plugin.sendToDiscord(response, channel)
            if (event.isFromRelayChannel())
                plugin.sendToMinecraft(plugin.toMinecraftChatMessage(response, plugin.cfg.BOT_MC_USERNAME))
            return
        }
    }

    private fun MessageReceivedEvent.isFromRelayChannel(): Boolean {
        return this.guild.id == plugin.cfg.SERVER_ID
                && this.isFromType(ChannelType.TEXT)
                && this.textChannel.name.equals(plugin.cfg.CHANNEL, true)
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    fun onUnexpectedError(ws: WebSocket, wse: WebSocketException) {
        plugin.logger.severe("Unexpected error from DiscordBridge: ${wse.message}")
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    fun onDisconnected(webSocket: WebSocket, serverCloseFrame: WebSocketFrame, clientCloseFrame: WebSocketFrame, closedByServer: Boolean) {
        plugin.logDebug("Discord disconnected - attempting to reconnect")
        connection.reconnect()
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> List<*>.checkItemsAre() = if (all { it is T }) this as List<T> else null
}
