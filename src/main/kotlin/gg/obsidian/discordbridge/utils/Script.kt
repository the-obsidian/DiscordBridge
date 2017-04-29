package gg.obsidian.discordbridge.utils

import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.configuration.serialization.SerializableAs

/**
 * Represents a Script entry in script.yml
 *
 * @param triggerMC the string that, if sent from Minecraft, will trigger this response
 * @param triggerDis the string that, if sent from Discord, will trigger this response
 * @param responseMC the response that will be broadcast to Minecraft if this script is triggered
 * @param responseDis the response that will be broadcast to Discord if this script is triggered
 * @param caseSensitive whether the trigger strings are case sensitive
 * @param requiresMention whether the trigger strings can only be activated by @tag mentioning the bot
 * @param startsWith whether the trigger strings only need to match the start of the string
 */
@SerializableAs("Script")
data class Script(val triggerMC: String?, val triggerDis: String?, val responseMC: String?, val responseDis: String?,
                  val caseSensitive: Boolean?, val requiresMention: Boolean?,
                  val startsWith: Boolean?): ConfigurationSerializable {
    @Suppress("unused")
    constructor(map: MutableMap<String, Any>):
            this(map["trigger-minecraft"] as? String, map["trigger-discord"] as? String, map["response-minecraft"] as? String,
                    map["response-discord"] as String, map["case-sensitive"] as? Boolean, map["requires-mention"] as? Boolean,
                    map["starts-with"] as? Boolean)

    override fun serialize(): MutableMap<String, Any> {
        val map: MutableMap<String, Any> = mutableMapOf()
        if (triggerMC != null) map.put("trigger-minecraft", triggerMC)
        if (triggerDis != null) map.put("trigger-discord", triggerDis)
        if (responseMC != null) map.put("respose-minecraft", responseMC)
        if (responseDis != null) map.put("response-discord", responseDis)
        if (caseSensitive != null) map.put("case-sensitive", caseSensitive)
        if (requiresMention != null) map.put("requires-mention", requiresMention)
        if (startsWith != null) map.put("starts-with", startsWith)

        return map
    }

}