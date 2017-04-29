package gg.obsidian.discordbridge.utils

import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.configuration.serialization.SerializableAs

//TODO: Make null safe
/**
 * Represents a Rating entry in rate.yml
 *
 * @param message the rating message template
 * @param low the lower bound of ratings that will trigger this message
 * @param high the upper bound of ratings that will trigger this message
 */
@SerializableAs("Rating")
data class Rating(val message: String = "%u - I rate %m %r", val low: Double = 0.0, val high: Double = 0.0): ConfigurationSerializable {
    @Suppress("unused")
    constructor(map: MutableMap<String, Any>):
            this(map["message"] as String, map["low"] as Double, map["high"] as Double)

    override fun serialize(): MutableMap<String, Any> {
        return mutableMapOf("message" to message, "low" to low, "high" to high)
    }

}