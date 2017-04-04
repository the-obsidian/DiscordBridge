package co.orre.discordbridge.utils

import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.configuration.serialization.SerializableAs

//TODO: make null safe
@SerializableAs("Respect")
data class Respect(val message: String = "%u solemnly pays respect!", val count: Int = 1, val weight: Int = 1): ConfigurationSerializable {
    @Suppress("unused")
    constructor(map: MutableMap<String, Any>):
            this(map["message"] as String, map["respects-paid"] as Int, map["weight"] as Int)

    override fun serialize(): MutableMap<String, Any> {
        return mutableMapOf("message" to message, "respects-paid" to count, "weight" to weight)
    }

}