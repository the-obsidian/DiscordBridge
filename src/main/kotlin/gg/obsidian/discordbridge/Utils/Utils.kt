package gg.obsidian.discordbridge.Utils

import org.bukkit.ChatColor
import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.configuration.serialization.SerializableAs

fun String.noSpace() = this.replace(Regex("""\s+"""), "")
fun String.stripColor(): String = ChatColor.stripColor(this)

data class UserAlias(var mcUsername: String, var mcUuid: String, var discordUsername: String, var discordId: String)

//TODO: make null safe
@SerializableAs("Respect")
data class Respect(val message: String = "%u solemnly pays respect!", val count: Int = 1, val weight: Int = 1): ConfigurationSerializable{
    @Suppress("unused")
    constructor(map: MutableMap<String, Any>):
            this(map["message"] as String, map["respects-paid"] as Int, map["weight"] as Int)

    override fun serialize(): MutableMap<String, Any> {
        return mutableMapOf("message" to message, "respects-paid" to count, "weight" to weight)
    }

}

//TODO: Make null safe
@SerializableAs("Rating")
data class Rating(val message: String = "%u - I rate %m %r", val low: Double = 0.0, val high: Double = 0.0): ConfigurationSerializable{
    @Suppress("unused")
    constructor(map: MutableMap<String, Any>):
            this(map["message"] as String, map["low"] as Double, map["high"] as Double)

    override fun serialize(): MutableMap<String, Any> {
        return mutableMapOf("message" to message, "low" to low, "high" to high)
    }

}

@SerializableAs("Script")
data class Script(val triggerMC: String?, val triggerDis: String?, val responseMC: String?, val responseDis: String?,
                  val caseSensitive: Boolean?, val requiresMention: Boolean?,
                  val startsWith: Boolean?): ConfigurationSerializable{
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