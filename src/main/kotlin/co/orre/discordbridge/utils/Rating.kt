package co.orre.discordbridge.utils

import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.configuration.serialization.SerializableAs

//TODO: Make null safe
@SerializableAs("Rating")
data class Rating(val message: String = "%u - I rate %m %r", val low: Double = 0.0, val high: Double = 0.0): ConfigurationSerializable {
    @Suppress("unused")
    constructor(map: MutableMap<String, Any>):
            this(map["message"] as String, map["low"] as Double, map["high"] as Double)

    override fun serialize(): MutableMap<String, Any> {
        return mutableMapOf("message" to message, "low" to low, "high" to high)
    }

}