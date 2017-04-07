package co.orre.discordbridge.utils

import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.configuration.serialization.SerializableAs

//data class UserAlias(var mcUsername: String, var mcUuid: String, var discordUsername: String, var discordId: String)

//TODO: make null safe
@SerializableAs("UserAlias")
data class UserAlias(val mcUuid: String, val discordId: String): ConfigurationSerializable {
    @Suppress("unused")
    constructor(map: MutableMap<String, Any>):
            this(map["mcUuid"] as String, map["discordId"] as String)

    override fun serialize(): MutableMap<String, Any> {
        return mutableMapOf("mcUuid" to mcUuid, "discordId" to discordId)
    }

}